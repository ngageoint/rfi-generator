import org.junit.*;
import java.util.*;
import models.*;
import helpers.*;
import notifiers.*;
import javax.*;

import play.*;
import play.db.jpa.*;
import play.mvc.*;
import play.test.*;
import play.libs.*;

public class EmailTests extends UnitTest {
    //TODO: Expand these to test multiple email sending (ex. handleRFIupdated logic)
    @Test
    public void testAllEmailSending() {
        RFI r = RFI.findById(1L);
        Comment c = r.comments.get(0);
        Event e = r.event;
        RelatedRFIItem rel = r.relatedItems.get(0);
        
        Mails.commentCreated(c, "test@test.com");
        Mails.eventCreated(e, "test@test.com");
        Mails.eventGroupUpdated(e, "test@test.com");
        Mails.rfiCreated(r, "test@test.com");
        Mails.groupChanged(r, "test@test.com");
        Mails.rfiAssigned(r, "test@test.com");
        Mails.sendStatusChange(r, "test@test.com");
        Mails.sendProductAdded(rel, "test@test.com");
        
        // For testing classification strings
        RFI r2 = RFI.findById(2L);
        Mails.rfiCreated(r2, "test@test.com");
        
        RFI activityRFI = RFI.findById(17L);
        Mails.rfiCreated(activityRFI, "test@test.com");
        Event activityEvent = activityRFI.event;
        Mails.eventCreated(activityEvent, "test@test.com");
    }
    
    @Test
    public void testEmailClassificationFormatting() {
        RFI r = RFI.findById(2L);
        String [] cls = RFIHelpers.gatherClassificationEmailStrings(r);
        assertTrue(cls.length == 2);
        Logger.debug(cls[0]);
        Logger.debug(cls[1]);
        
        RFI r2 = RFI.findById(1L);
        String [] cls2 = RFIHelpers.gatherClassificationEmailStrings(r2);
        assertTrue(cls.length == 2);
        assertEquals(cls2[0], "CLASSIFICATION COULD BE UP TO Unclassified//FOUO");
    }
}
