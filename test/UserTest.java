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

public class UserTest extends UnitTest {
    @Test
    public void testUserRoleFind() throws Exception {
        assertEquals(ConfigurationHelpers.getMagicRoleForUsername("admin").name, "Field");
        assertEquals(ConfigurationHelpers.getMagicRoleForUsername("demo").name, "Analyst");
    }
}
