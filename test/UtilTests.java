import org.junit.*;
import org.junit.rules.*;
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

public class UtilTests extends UnitTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testURLExtract() {
        String src = "http://www.google.com <- web link";
        assertTrue(ApplicationHelpers.extractURL(src).equals("http://www.google.com"));
    }
    
    @Test
    public void testLinkify() {
        String src = "http://www.google.com";
        assertEquals(ApplicationHelpers.linkify(src), "<a href=\"http://www.google.com\">http://www.google.com</a>");
        
        String emailSrc = "joe@test.com; joe2@test.com";
        assertEquals(ApplicationHelpers.linkify(emailSrc), "<a href=\"mailto:joe@test.com\">joe@test.com</a>; <a href=\"mailto:joe2@test.com\">joe2@test.com</a>");
    }
    
    @Test
    public void testValidEmail() {
        assertTrue(ApplicationHelpers.isValidEmail("joe@test.com"));
        assertTrue(ApplicationHelpers.isValidEmail("joe@test.com.au"));
        assertTrue(ApplicationHelpers.isValidEmail("joe.mctester@test.com.au"));
        assertTrue(ApplicationHelpers.isValidEmail("joe.mctester_ctr@test.com.au"));
        assertFalse(ApplicationHelpers.isValidEmail("joe@testcom"));
        assertFalse(ApplicationHelpers.isValidEmail("joe.testcom"));
        assertFalse(ApplicationHelpers.isValidEmail(""));
        
        exception.expect(NullPointerException.class);
        ApplicationHelpers.isValidEmail(null);
    }
}
