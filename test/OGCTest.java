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
import java.io.*;
import play.libs.*;

import javax.xml.parsers.*;

import flexjson.*;
import flexjson.transformer.*;

public class OGCTest extends UnitTest {
    @Test
    public void testOGCInsert() throws Exception {
        // Region
        RFI r = RFI.findById(4L);
        OGCPublishing.insertRFI(r);
        
        // Polyline
        r = RFI.findById(6L);
        OGCPublishing.insertRFI(r);
    }
    
    @Test
    public void testOGCUpdate() throws Exception {
        // Update region to point
        RFI tr = RFI.findById(4L);
        RFI r = RFI.findById(3L);
        tr.coordinates = r.coordinates;
        OGCPublishing.updateRFI(tr);
    }
    
    @Test
    public void testOGCDelete() throws Exception {
        RFI r = RFI.findById(1L);
        OGCPublishing.deleteRFI(r);
    }
}
