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

public class ConfigurationTests extends UnitTest {
    @Test
    public void testGetClassificationLevels() {
        Map<String,Map> levels = ConfigurationHelpers.getClassificationLevels();
        Map unc = levels.get("Level1");
        assertTrue(unc.get("level").equals(1));
        
        Map unc2 = levels.get("Level3");
        assertTrue((Integer)unc2.get("level") > (Integer)unc.get("level"));
    }
}
