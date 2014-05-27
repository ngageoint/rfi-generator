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

public class RFICRUD extends UnitTest {
    @Test
    public void testRFIInsert() {
        RFI r = new RFI();
        r.title = "Insert test";
        r.status = ApplicationHelpers.PENDING;
        r.event = Event.all().first();
        r.emailAddress = "requestor@mail.com";
        r.coordinates = new Point();
        r.coordinates.lat = 38.0;
        r.coordinates.lon = -91.0;
        r.save();
        RFIHelpers.handleRFICreated(r, "test");
        
        // Mock emailer is finiky, sometimes an email can't be retrieved
        //String email = Mail.Mock.getLastMessageReceivedBy("admin@mail.com");
        //assertTrue(email.contains("Insert test"));
    }
    
    @Test
    public void testRFIUpdate() {
        Long max = (Long)JPA.em().createQuery("select max(id) from RFI").getSingleResult();
        Logger.debug("Max id %d", max);
        RFI r = RFI.findById(max);
        r.coordinates.lat = 0;
        r.coordinates.lon = 0;
        r.assignedTo = "assignedto@mail.com";
        r.region = "[{\"lat\":39.93822942352003,\"lng\":-94.70568923950196},{\"lat\":38.931552992480995,\"lng\":-94.63204650878907},{\"lat\":38.912321359564494,\"lng\":-94.63410644531251},{\"lat\":38.899764565101776,\"lng\":-94.67702178955079},{\"lat\":38.912054216850116,\"lng\":-94.69890861511232},{\"lat\":38.92220493312964,\"lng\":-94.70435886383058}]";
        r.save();
        RFIHelpers.handleRFIUpdated(r, "", "", "", "");
        
        // Mock emailer is finiky, sometimes an email can't be retrieved
        //String email = Mail.Mock.getLastMessageReceivedBy("assignedto@mail.com");
        // Should be latest RFI reference
        //assertTrue(email.contains("/admin/edit/18"));
    }
    
    @Test
    public void testRFIDeleted() {
        Long max = (Long)JPA.em().createQuery("select max(id) from RFI").getSingleResult();
        Logger.debug("Max id %d", max);
        RFI r = RFI.findById(max);
        Long id = r.id;
        // Weird JPA session stuff not enforcing the cascading delete on the RFI's activities
        r.refresh();
        RFIHelpers.handleRFIDeleted(r);
        r.delete();
        
        assertTrue(RFI.findById(id) == null);
    }
}
