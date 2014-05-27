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

public class BasicTest extends UnitTest {
    @Test
    public void testRFIDeserialization() {
        List<RFI> rfis = RFI.getAll();
        String res = ApplicationHelpers.serialize(rfis);
        assertTrue(res.length() > 0);
    }
    
    @Test
    public void getGEONAMESTestXML() {
        String ret = GEONAMESHelper.QueryAndTransformToJSON("Saint Louis");
        assertNotNull(ret);
    }
    
    @Test
    public void getAllProductTypes() {
        String [] ret = RFIHelpers.getAllProductFormats();
        Logger.info(Arrays.toString(ret));
    }
    
    @Test
    public void serializeMap() {
        Map<String, Object> obj = new HashMap<String, Object>();
        List<String> listOfStrings = new ArrayList<String>();
        listOfStrings.add("HELLO!");
        listOfStrings.add("THERE!");
        
        obj.put("HI", 11);
        obj.put("list", listOfStrings);
        
        String ret = new JSONSerializer().deepSerialize(obj);
        
        Map<String,Object> obj2 = new JSONDeserializer<Map<String,Object>>().deserialize(ret);
        
        assertTrue(((List<String>)obj2.get("list")).size() == 2);
    }
}
