package controllers;

import helpers.*;

import play.*;
import play.libs.*;
import play.mvc.*;
import play.db.jpa.*;
import play.data.validation.*;

import java.util.*;
import java.io.*;

import models.*;

import flexjson.*;

import controllers.deadbolt.*;

import javax.persistence.*;
import javax.persistence.criteria.*;

// Used for the event routes in the map experience
@With({Master.class, Deadbolt.class})
public class EventController extends BaseController {
    static Event getFromJSON(String body) {
        Event r = new JSONDeserializer<Event>().use(null, Event.class)
            .use("createdAt", new IgnoreObjectFactory())
            .deserialize(body);
        return r;
    }
    
    // POST /rfi - JSON route for creating event
    @Restrict("!Field")
    public static void createJSON(String body) {
        Event e = getFromJSON(body);
        _create(e, null, null, true);
    }
    
    // PUT /rfi/[ID] - JSON route for updating existing event
    @Restrict("!Field")
    public static void updateJSON(Long id, String body) {
        Event cur = Event.findById(id);
        Event e = getFromJSON(body);
        EventHelpers.copyLeft(cur,e);
        _update(cur, null, true, null);
    }

    // /event/edit/[id] - Event editing dialog
    @Restrict("!Field")
    public static void edit(Long id) {
        Event e = Event.findById(id);
        if ( e == null) {
            notFound("Event not found");
        }
        render(e);
    }
    
    // POST /event/edit/[id] - form post, update event
    @Restrict("!Field")
    public static void update(@Valid Event e, Boolean createGeoQProject, EventActivity newActivity) {
        _update(e, createGeoQProject, false, newActivity);
    }
    
    static void _update(Event e, Boolean createGeoQProject, boolean json, EventActivity newActivity) {
        Event.em().detach(e);
        Event old = Event.findById(e.id);
        Event.em().detach(old);
        if ( !validation.hasErrors()) {
            flash.success("Event updated");
            e = Event.em().merge(e);
            e.save();
            Logger.debug("%s", newActivity);
            
            if ( newActivity != null && newActivity.name != null && !newActivity.name.trim().equals("")) {
                newActivity.save();
                e.eventActivities.add(newActivity);
                e.save();
            }
            
            if ( createGeoQProject != null && createGeoQProject && (e.geoQId == null || e.geoQId.equals(""))) {
                GEOQIntegration.putGeoQProject(e);
            }
            
            EventHelpers.handleEventUpdated(e, Master.connected(), old);
            
            if ( json) {
                serializeEvents(e);
            }
        }
        else {
            ApplicationHelpers.setResponseError(response);
            
            if ( json) {
                Map<String, List> errors = new HashMap<String,List>();
                errors.put("errors", ApplicationHelpers.getErrors(validation.errors()));
                serializeErrors(errors);
            }
            else {
                renderTemplate("EventController/edit.html", e);
            }
        }
    }
    
    @Restrict("!Field")
    public static void newEvent() {
        Event e = new Event();
        render(e);
    }
    
    @Restrict("!Field")
    public static void updateRegion(Long id, String region) {
        Event evt = Event.findById(id);
        
        if ( evt != null) {
            evt.region = region;
            evt.save();
        }
    }
    
    @Restrict("!Field")
    public static void create(@Valid Event e, String archived, Boolean createGeoQProject) {
        _create(e, archived, createGeoQProject, false);
    }
    
    static void _create(Event e, String archived, Boolean createGeoQProject, boolean json) {
        if ( archived == null) {
            e.archived = false;
        }
        else {
            e.archived = true;
        }
        if ( !validation.hasErrors()) {
            e.createdBy = Master.connected();
            e.uuid = UUID.randomUUID().toString();
            e.save();
            EventHelpers.handleEventCreated(e, Master.connected());
            if ( createGeoQProject != null && createGeoQProject && (e.geoQId == null || e.geoQId.equals(""))) {
                GEOQIntegration.putGeoQProject(e);
            }
            flash.success("Created new event");
            serializeEvents(e);
        }
        else {
            ApplicationHelpers.setResponseError(response);
            if (json) {
                Map<String, List> errors = new HashMap<String,List>();
                errors.put("errors", ApplicationHelpers.getErrors(validation.errors()));
                serializeErrors(errors);
            }
            else {
                renderTemplate("EventController/newEvent.html", e);
            }
        }
    }
    
    // Helper for gridding out events on the map experience
    public static void grid(int page, String sort, String sortDirection) {
        //TODO: Updated to refactored version
        if ( sort == null) {
            sort = "createdAt";
        }
        if ( sortDirection == null) {
            sortDirection = "desc";
        }
        
        int page_size = ApplicationHelpers.DEFAULT_PAGE_SIZE;
        
        String _params = String.format("page=%d&sort=%s&sortDirection=%s", page, sort, sortDirection);
        
        CriteriaBuilder builder = JPA.local.get().em().getCriteriaBuilder();
        CriteriaQuery qry = builder.createQuery(Event.class);
        Root<Event> eventLoad = qry.from(Event.class);
        Predicate notArchived = builder.and(
            builder.or(builder.notEqual(eventLoad.get("archived"), 1), builder.isNull(eventLoad.get("archived"))));
        qry.where(notArchived);
        if ( sortDirection.equals("asc")) {
            qry.orderBy(builder.asc(eventLoad.get(sort)));
        }
        else {
            qry.orderBy(builder.desc(eventLoad.get(sort)));
        }
        
        //WTF...doesn't the render method mixin the class' static scope???
        Query query = JPA.local.get().em().createQuery(qry);
        List<Event> events = query.getResultList();
        long totalCount = events.size();
        
        render(events, totalCount, page_size, _params);
    }
    
    protected static List<Event> getActiveEvents() {
        return Event.getActive();
    }

    // /event/[ID].json - Get JSON for specified event
    public static void get(long id) {
        serializeEvents(Event.findById(id));
    }
    
    // /events - Get JSON for event list (can filter via querystring "event" and "active")
    public static void getAll(Long event, Boolean active) {
        if ( event != null) {
            List<JPABase> items = new ArrayList<JPABase>();
            items.add(Event.findById(event));
            serializeEvents(items);
        }
        else {
            if ( active != null && active == true) {
                serializeEvents(Event.getActive());

            }
            else {
                serializeEvents(Event.getAll());
            }
        }
    }
}
