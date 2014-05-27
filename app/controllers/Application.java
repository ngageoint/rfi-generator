package controllers;

import play.*;
import play.db.jpa.*;
import play.mvc.*;

import helpers.*;

import java.util.*;

import javax.persistence.*;
import javax.persistence.criteria.*;

import controllers.deadbolt.*;

import models.*;

import org.joda.time.*;

// For rendering the single full-page in the map experience (rest are rendered via dialogs)
@With({Master.class, Deadbolt.class})
public class Application extends BaseController {
    // Builds list of all requestors in the system
    static List<SelectContainer> buildRequestors() {
        Query q = JPA.em().createQuery("select distinct CONCAT(requestorFirstName, ' ', requestorLastName) from RFI order by CONCAT(requestorFirstName, ' ', requestorLastName) asc");
        List<String> r = q.getResultList();
        List<SelectContainer> requestors = new ArrayList<SelectContainer>();
        for ( String s: r) {
            requestors.add(new SelectContainer(s, s));
        }
        return requestors;
    }
    
    public static void index(Long event, Long rfi, Boolean noRedirect) {
        Boolean showSelection = true;
        if ( event != null) {
            session.put("event", event);
        }
        else {
            session.remove("event");
        }
        if ( session.contains("selection-shown") || session.get("event") != null || rfi != null) {
            showSelection = false;
        }
        else {
            session.put("selection-shown", "1");
        }
       
        RFI curRFI = null;
        // Load current RFI in context (querystring ?rfi=[ID])
        if ( rfi != null) {
            curRFI = RFI.findById(rfi);
        }
        
        User usr = Master.getUser();
        if ( usr.isAdmin() && curRFI == null && event == null && (noRedirect == null || noRedirect == false)) {
            AdminController.index(null);
        }
        
        List<Event> events = Event.getActive();
        List<SelectContainer> requestors = buildRequestors();
        requestors.add(0, new SelectContainer("Select a requestor...", ""));
        
        render(events, requestors, showSelection, curRFI);
    }
}
