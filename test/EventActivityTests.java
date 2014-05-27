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

public class EventActivityTests extends UnitTest {
    @Test
    public void testSaveCascades() {
        Event e = Event.findById(4L);
        EventActivity ea = new EventActivity();
        ea.name = "TEST";
        e.eventActivities.add(ea);
        e.save();
        
        EventActivity e_ea = EventActivity.find("select distinct ea from EventActivity ea join ea.events as e where ea.name = ? and e = ?", "TEST", e).first();
        
        assertTrue(e_ea != null);
    }
}
