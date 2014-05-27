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

public class SerializationTests extends UnitTest {
    @Test
    public void testRFISerialization() {
        List<RFI> rfis = RFI.getAll();
        String res = SerializationHelpers.getRFIFullSerializer().serialize(rfis);
        Logger.debug(res);
        
        List<RFI> deserialized = SerializationHelpers.getRFIFullDeserializer().deserialize(res);
        assertTrue(deserialized.size() == rfis.size());
        
        RFI first = (RFI)deserialized.get(0);
        RelatedRFIItem firstRel = (RelatedRFIItem)first.relatedItems.get(0);
        assertTrue(firstRel.text.equals("http://www.google.com <- web link"));
    }
}
