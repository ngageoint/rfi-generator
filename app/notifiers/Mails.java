package notifiers;
 
import play.*;
import play.mvc.*;
import java.util.*;

import helpers.*;

import models.*;

// Sends emails, mostly when event/rfi is created/updated
public class Mails extends Mailer {
    final static String from = play.Play.configuration.get("mail.from").toString();
    
    public static void commentCreated(Comment c, String email) {
        String baseUrl = ConfigurationHelpers.GetBaseURL();
        Logger.debug("Email");
        setSubject("RFI Generator - Comment created for RFI: %s", c.rfi.title);
        
        addRecipient(email);
        setFrom(from);
        
        send(c, baseUrl);
    }
    
    public static void eventCreated(Event e, String email) {
        String baseUrl = ConfigurationHelpers.GetBaseURL();

        setSubject("RFI Generator - Event created: %s", e.name);

        addRecipient(email);
        setFrom(from);
        
        String message = "A new event has been created with the following information:";

        send("Mails/event", e, baseUrl, message);
    }
    
    public static void eventGroupUpdated(Event e, String email) {
        String baseUrl = ConfigurationHelpers.GetBaseURL();

        setSubject("RFI Generator - Event's group updated: %s", e.name);

        addRecipient(email);
        setFrom(from);
        
        String message = "An event has been updated with the following information (associated group updated):";

        send("Mails/event", e, baseUrl, message);
    }
    
    public static void rfiCreated(RFI r, String email) {
        String baseUrl = ConfigurationHelpers.GetBaseURL();

        setSubject("RFI Generator - RFI created: %s", r.title);

        addRecipient(email);
        setFrom(from);
        
        String message = "A new RFI has been created with the following information:";
        Logger.debug("**********************Email");
        send("Mails/rfi", r, baseUrl, message);
    }
    
    public static void groupChanged(RFI r, String email) {
        String baseUrl = ConfigurationHelpers.GetBaseURL();
        
        setSubject("RFI Generator - RFI's group changed: %s", r.title);
        
        addRecipient(email);
        setFrom(from);
         
        String message = "The following RFI's group has changed:";
        
        send("Mails/rfi", r, baseUrl, message);
    }
    
    public static void rfiAssigned(RFI r, String email) {
        String baseUrl = ConfigurationHelpers.GetBaseURL();

        setSubject("RFI Generator - RFI has been assigned: %s", r.title);

        addRecipient(email);
        setFrom(from);
        
        String message = "A RFI has been assigned: ";

        send("Mails/rfi", r, baseUrl, message);
    }
    
    public static void sendStatusChange(RFI r, String email) {
        String baseUrl = ConfigurationHelpers.GetBaseURL();
        
        setSubject("RFI Generator - RFI status changed (%s)", r.status);
        addRecipient(email);
        
        setFrom(from);
        
        String message = String.format("The RFI (%s) has changd status to %s:", r.title, r.status);
        
        send("Mails/rfi", r, baseUrl, message);
    }
    
    public static void sendProductAdded(RelatedRFIItem rel, String email) {
        String baseUrl = ConfigurationHelpers.GetBaseURL();
        
        RFI r = RFI.findById(rel.rfi.id);
        
        setSubject("RFI Generator - Product URL added to RFI (%s)", r.status);
        addRecipient(email);
        setFrom(from);
        
        String message = "A product has been added to the following RFI:";
        
        send("Mails/rfi", r, baseUrl, message);
    }
}
