import org.junit.*;
import java.util.*;
import play.test.*;
import models.*;
import helpers.*;
import javax.*;
import play.*;
import play.db.jpa.*;
import play.mvc.*;

import org.w3c.dom.*;
import org.xml.sax.*;
import com.google.gson.*;
import java.io.*;
import play.libs.*;

import javax.xml.parsers.*;

import flexjson.*;
import flexjson.transformer.*;

public class GEOQIntegrationTests extends UnitTest {
    @Test
    public void testProjectReadFromGEOQ() {
        JsonElement s = GEOQIntegration.getGeoQProjects();
        Logger.info(s.toString());
        assertTrue(s.isJsonArray());
    }
    
    @Test
    public void testRegionToWKTPolygon() {
        Event e = Event.find("byName", "Tornadoes in Kansas City, MO").first();
        String poly = GEOQIntegration.regionToWKTPolygon(e.region);
        assertEquals(poly, "POLYGON ((-94.789288 39.043900 , -94.426740 39.048166 , -94.330609 38.911520 , -94.459698 38.661032 , -94.995282 38.563382 , -95.146344 38.701769 , -95.052960 39.005493 ))");
    }
    
    @Test
    public void putGeoQProject() {
        Event e = Event.find("byName", "Tornadoes in Kansas City, MO").first();
        GEOQIntegration.putGeoQProject(e);
    }
    
    @Test
    public void testBufferCreation() {
        RFI r = RFI.findById(2L);
        GEOQIntegration.putGeoQJob(r);
    }
}
