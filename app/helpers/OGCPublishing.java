package helpers;

import models.*;

import play.*;
import play.data.validation.*;
import play.db.jpa.*;
import play.vfs.*;
import play.utils.*;
import play.libs.*;

import org.apache.commons.lang.*;

import java.util.regex.*;
import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import java.text.*;

import flexjson.*;

import javax.persistence.*;
import javax.persistence.criteria.*;

/*
For publishing RFIs to a geoserver, any-SPATIAL RFI (point/line/poly) is able to
be pushed to GeoServer.  See the readme.md for geoserver setup.  Configuration
files (wfsInsert.xml, wfsUpdate.xml and wfsDelete.xml) are the templates used
for pushing features over WFST.
*/
public class OGCPublishing {
    public static String insert = VirtualFile.fromRelativePath("conf/wfsInsert.xml").contentAsString();
    public static String update = VirtualFile.fromRelativePath("conf/wfsUpdate.xml").contentAsString();
    public static String delete = VirtualFile.fromRelativePath("conf/wfsDelete.xml").contentAsString();
    public static String wfsURL = Play.configuration.getProperty("wfs.URL").toString();

    public static void insertRFI(RFI r) throws Exception {
        if ( r.nonSpatial == null || !r.nonSpatial) {
            WS.WSRequest req = WS.url(wfsURL);
            String b = processInsert(r);
            Logger.debug(b);
            req.body = b;
            WS.HttpResponse resp = req.post();
            Logger.debug(resp.getString());
            if ( resp.getString().contains("ows:ExceptionReport")) {
                throw new Exception(resp.getString());
            }
        }
    }
    
    public static void updateRFI(RFI r) throws Exception {
        if ( r.nonSpatial == null || !r.nonSpatial) {
            WS.WSRequest req = WS.url(wfsURL);
            req.body = processUpdate(r);
            WS.HttpResponse resp = req.post();
            Logger.debug(resp.getString());
            if ( resp.getString().contains("ows:ExceptionReport")) {
                throw new Exception(resp.getString());
            }
        }
    }
    
    public static void deleteRFI(RFI r) throws Exception {
        if ( r.nonSpatial == null || !r.nonSpatial) {
            WS.WSRequest req = WS.url(wfsURL);
            req.body = processDelete(r);
            WS.HttpResponse resp = req.post();
            Logger.debug(resp.getString());
            if ( resp.getString().contains("ows:ExceptionReport")) {
                throw new Exception(resp.getString());
            }
        }
    }
    
    static String getFeatureGML(RFI r) {
        String gml = null;
        if ( r.region != null && !r.region.equals("")) {
            gml = polyFromJSON(r.region);
        }
        else if ( r.polyline != null && !r.polyline.equals("")) {
            gml = polylineFromJSON(r.polyline);
        }
        else if ( r.coordinates != null && (r.coordinates.lat != 0 || r.coordinates.lon != 0)) {
            gml = pointFromPoint(r.coordinates);
        }
        return gml;
    }
    
    static String processInsert(RFI r) {
        return String.format(insert, r.id, getFeatureGML(r));
    }
    
    static String processUpdate(RFI r) {
        return String.format(update, getFeatureGML(r), r.id);
    }
    
    static String processDelete(RFI r) {
        return String.format(delete, r.id);
    }
    
    static String polyFromJSON(String region) {
        JSONDeserializer ser = new JSONDeserializer();
        ArrayList<Map<String,Object>> dict = (ArrayList<Map<String,Object>>)ser.deserialize(region);
        String templ = "<gml:Polygon srsName=\"EPSG:4326\"><gml:exterior><gml:LinearRing><gml:posList>%s</gml:posList></gml:LinearRing></gml:exterior></gml:Polygon>";
        StringBuilder bld = new StringBuilder();
        for ( Map<String,Object> pt: dict) {
            bld.append(String.format("%f %f ", pt.get("lng"), pt.get("lat")));
        }
        Map<String,Object> f = (Map<String,Object>)dict.get(0);
        bld.append(String.format("%f %f", f.get("lng"), f.get("lat")));
        return String.format(templ, bld.toString());
    }
    
    static String polylineFromJSON(String polyline) {
        JSONDeserializer ser = new JSONDeserializer();
        ArrayList<Map<String,Object>> dict = (ArrayList<Map<String,Object>>)ser.deserialize(polyline);
        String templ = "<gml:LineString srsName=\"EPSG:4326\"><gml:posList>%s</gml:posList></gml:LineString>";
        StringBuilder bld = new StringBuilder();
        for ( Map<String,Object> pt: dict) {
            bld.append(String.format("%f %f ", pt.get("lng"), pt.get("lat")));
        }
        return String.format(templ, bld.toString());
    }
    
    static String pointFromPoint(Point coord) {
        String templ = "<gml:Point srsName=\"EPSG:4326\"><gml:pos>%f %f</gml:pos></gml:Point>";
        return String.format(templ, coord.lon, coord.lat);
    }
}
