package helpers;

import models.RFI;
import models.Event;

import play.*;
import play.data.validation.*;
import play.db.jpa.*;
import play.libs.*;
import play.vfs.*;

import com.google.gson.*;

import java.util.regex.*;
import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import java.text.*;

import org.apache.commons.lang.*;

import com.vividsolutions.jts.geom.*;
import org.opengis.referencing.operation.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.*;
import org.geotools.referencing.*;
import org.geotools.geometry.jts.JTS;

import javax.persistence.*;
import javax.persistence.criteria.*;

import flexjson.*;
import flexjson.transformer.*;

public class GEOQIntegration {
    public static final String url = Play.configuration.getProperty("geoQ.baseURL", "");
    public static final String user = Play.configuration.getProperty("geoQ.username", "");
    public static final String pass = Play.configuration.getProperty("geoQ.password", "");

    public static JsonElement getGeoQProjects() {
        if ( !url.equals("")) {
            WS.WSRequest req = WS.url(url + "/projects.json");
            req.authenticate(user, pass);
            WS.HttpResponse resp = req.get();
            return resp.getJson();
        }
        
        return null;
    }
    
    /*{
"boundary": "POLYGON ((-94.7892883301 39.0439002396, -94.426739502 39.0481664511, -94.3306091309 38.9115199284, -94.4596984863 38.6610318298, -94.9952819824 38.5633822609, -95.1463439941 38.7017692241, -95.0529602051 39.0054927431))",
"description": "Joe's Test",
"name": "Joe's Event"
}*/
    
    public static void putGeoQProject(Event e) {
        if ( !url.equals("")) {
            try {
                WS.WSRequest req = WS.url(url + "/projects.json");
                req.authenticate(user, pass);
                req.setHeader("Content-type", "application/json");
                JSONSerializer ser = new JSONSerializer();
                Map<String, String> m = new HashMap<String, String>();
                m.put("boundary", regionToWKTPolygon(e.region));
                m.put("project_type", "Other");
                if ( e.description.length() < 15) {
                    e.description = String.format("%1$-15s", e.description);
                }
                m.put("description", e.description);
                m.put("name", e.name);
                String body = ser.serialize(m);
                req.body(body);
                Logger.debug(body);
                WS.HttpResponse resp = req.post();
                JsonObject jResp = (JsonObject)resp.getJson();
                JsonPrimitive id = (JsonPrimitive)jResp.get("id");
                e.geoQId = id.getAsString();
                e.save();
            } catch (Exception ex) {
                // Fail silently
                Logger.error(ex, "GEOQ exception putting Event -> Project");
            }
        }
    }
    
    public static void putGeoQJob(RFI r) {
        try {
            CoordinateReferenceSystem WGS = CRS.decode("EPSG:4326", true);
            CoordinateReferenceSystem spMerc = CRS.decode("EPSG:3857", true);
            
            GeometryFactory f = new GeometryFactory(new PrecisionModel(), 4326);
            
            MathTransform transformTospMerc = CRS.findMathTransform(WGS, spMerc);
            MathTransform transformToWGS = CRS.findMathTransform(spMerc, WGS);
            
            Logger.debug("EVENT GEQID: %s", r.event.geoQId);
            Logger.debug("RFI POINT: %s", r.coordinates.toString());
            
            Map<String,String> m = new HashMap<String,String>();
            
            if ( r.event.geoQId != null && !r.event.geoQId.equals("")) {
                if ( r.nonSpatial == null || !r.nonSpatial) {
                    m.put("name", String.format("RFI - %s", r.networkID));
                    m.put("description", r.title);
                
                    // Sending 4326 boundary = to polygon/polyline appears to be working as-is
                    if ( r.region != null && !r.region.equals("")) {
                        m.put("boundary", regionToWKTPolygon(r.region));
                    }
                    else if ( r.polyline != null && !r.polyline.equals("")) {
                        m.put("boundary", regionToWKTPolygon(r.polyline));
                    }
                    else {
                        // Point - buffer by 1km
                        Point p = f.createPoint(new Coordinate(r.coordinates.lon, r.coordinates.lat));
                        Geometry buffer = JTS.transform(p, transformTospMerc).buffer(1000);
                        Geometry geom = JTS.transform(buffer, transformToWGS);
                        Logger.debug(p.toString());
                        Logger.debug(geom.toString());
                        m.put("boundary", geom.toString());
                    }
                    
                    String postURL = url + String.format("/projects/%s/jobs.json", r.event.geoQId);
                    WS.WSRequest req = WS.url(postURL);
                    req.authenticate(user, pass);
                    req.setHeader("Content-type", "application/json");
                    JSONSerializer ser = new JSONSerializer();
                    String body = ser.serialize(m);
                    Logger.debug("POST BODY: %s", body);
                    req.body(body);
                    WS.HttpResponse resp = req.post();
                    JsonObject jResp = (JsonObject)resp.getJson();
                    JsonPrimitive id = (JsonPrimitive)jResp.get("id");
                    r.geoQJobId = id.getAsString();
                    r.save();
                }
            }
        } catch (Exception e) {
            // Fail silently
            Logger.error(e, "GEOQ exception putting RFI -> Job");
        }
    }
    
    public static String regionToWKTPolygon(String region) {
        JSONDeserializer ser = new JSONDeserializer();
        ArrayList<Map<String,Object>> dict = (ArrayList<Map<String,Object>>)ser.deserialize(region);
        List<String> parts = new ArrayList<String>();
        for ( Map<String,Object> pt: dict) {
            parts.add(String.format("%f %f ", pt.get("lng"), pt.get("lat")));
        }
        return "POLYGON ((" + StringUtils.join(parts, ", ") + "))";
    }
}
