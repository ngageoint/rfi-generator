package controllers;

import play.*;
import play.db.jpa.*;
import play.modules.pdf.PDF.Options;
import play.mvc.*;
import play.libs.*;
import play.data.*;
import play.data.binding.*;
import play.data.validation.*;
import play.templates.Template;
import play.templates.TemplateLoader;
import play.vfs.*;

import helpers.*;
import notifiers.*;

import java.io.*;
import java.util.*;
import java.text.*;
import java.lang.reflect.*;
import java.util.regex.*;
import java.util.concurrent.atomic.*;

import javax.persistence.*;
import javax.persistence.criteria.*;

import controllers.deadbolt.*;

import models.*;

import org.joda.time.*;
import org.joda.time.format.*;

import org.yaml.snakeyaml.Yaml;

import flexjson.JSONSerializer;
import flexjson.JSONDeserializer;
import flexjson.transformer.*;

import org.apache.commons.io.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import static play.modules.pdf.PDF.*;

/* The admin controller is responsible for most actions with the /admin/* URL subset */
@With({Master.class, Deadbolt.class})
public class AdminController extends BaseController {
    public static void index(Long event) {
        //TODO: Barf
        dashboard(event, null, null, null, null, null, 0L);
    }
    
    // /admin/rfis or "RFIs" page in the admin experience
    public static void rfis(Boolean active, @As(",") List<String> sort,
        @As(",") List<String> sortDirection, String filterRequestorFirstName, String filterRequestorLastName,
        Long filterEvent, String filterOrganization, String filterAssignee, String filterSearch,
        String filterTitle, @As("yyyy-MM-dd") Date startDate, @As("yyyy-MM-dd") Date endDate,
        String filterInstructions, String filterCity, String filterComment, String filterID,
        String filterCountry, String filterBE, String filterStatus, long filterEventActivity, String filterProduct)
    {
        // Miss C#'s nice pass by reference...Java solution = AtomicReference
        AtomicReference<List<String>> _sort = new AtomicReference<List<String>>(sort);
        AtomicReference<List<String>> _sortDirection = new AtomicReference<List<String>>(sortDirection);
        ApplicationHelpers.sanitizeSortInfo(_sort, _sortDirection, 3);
        sort = _sort.get();
        sortDirection = _sortDirection.get();
        
        List<RFI> pendingRFIs = (List<RFI>)RFIHelpers.filter(sort.get(0),sortDirection.get(0),0,Integer.MAX_VALUE,active,filterRequestorFirstName,filterRequestorLastName,filterEvent,filterOrganization, filterAssignee, filterSearch, filterTitle, startDate, endDate, filterInstructions, filterCity, filterComment, filterID, null, null, filterCountry, filterBE, filterStatus, filterEventActivity, filterProduct).get("rfis");
        List<RFI> activeRFIs = (List<RFI>)RFIHelpers.filter(sort.get(1),sortDirection.get(1),0,Integer.MAX_VALUE,active,filterRequestorFirstName,filterRequestorLastName,filterEvent,filterOrganization, filterAssignee, filterSearch, filterTitle, startDate, endDate, filterInstructions, filterCity, filterComment, filterID, null, null, filterCountry, filterBE, filterStatus, filterEventActivity, filterProduct).get("rfis");
        List<RFI> completedRFIs = (List<RFI>)RFIHelpers.filter(sort.get(2),sortDirection.get(2),0,Integer.MAX_VALUE,active,filterRequestorFirstName,filterRequestorLastName,filterEvent,filterOrganization, filterAssignee, filterSearch, filterTitle, startDate, endDate, filterInstructions, filterCity, filterComment, filterID, null, null, filterCountry, filterBE, filterStatus, filterEventActivity, filterProduct).get("rfis");
        
        //TODO: I hate doing things like this...commons should have some nice clean ways to do this
        List<RFI> pending_rfis = new ArrayList<RFI>();
        for ( RFI r: pendingRFIs) {
            if ( r.isPending()) {
                pending_rfis.add(r);
            }
        }
        
        List<RFI> active_rfis = new ArrayList<RFI>();
        for ( RFI r: activeRFIs) {
            if ( r.isActive()) {
                active_rfis.add(r);
            }
        }
        
        List<RFI> completed_rfis = new ArrayList<RFI>();
        for ( RFI r: completedRFIs) {
            if ( r.isClosed()) {
                completed_rfis.add(r);
            }
        }
        
        render( pending_rfis, active_rfis, completed_rfis, active, filterRequestorFirstName, filterRequestorLastName, filterEvent);
    }
    
    // /admin/myrfis or "My RFIs" page in the admin experience
    public static void myRFIs(long event, @As(",") List<String> sort, @As(",") List<String> sortDirection) {
        User me = Master.getUser();
        List<RFI> _directAssigned = new ArrayList<RFI>();
        List<RFI> directAssigned_pending = new ArrayList<RFI>();
        List<RFI> directAssigned_active = new ArrayList<RFI>();
        List<RFI> directAssigned_completed = new ArrayList<RFI>();
        List<RFI> _inMyGroup = new ArrayList<RFI>();
        List<RFI> inMyGroup_pending = new ArrayList<RFI>();
        List<RFI> inMyGroup_active = new ArrayList<RFI>();
        List<RFI> inMyGroup_completed = new ArrayList<RFI>();
        AtomicReference<List<String>> _sort = new AtomicReference<List<String>>(sort);
        AtomicReference<List<String>> _sortDirection = new AtomicReference<List<String>>(sortDirection);
        ApplicationHelpers.sanitizeSortInfo(_sort, _sortDirection, 6);
        sort = _sort.get();
        sortDirection = _sortDirection.get();
        
        Event filteredEvent = Event.findById(event);
        
        _directAssigned = RFIHelpers.getDirectRFIs(me, filteredEvent, sort.get(0), sortDirection.get(0));
        for ( RFI r:_directAssigned) {
            if ( r.isPending()) {
                directAssigned_pending.add(r);
            }
        }
        
        _directAssigned = RFIHelpers.getDirectRFIs(me, filteredEvent, sort.get(1), sortDirection.get(1));
        for ( RFI r:_directAssigned) {
            if ( r.isActive()) {
                directAssigned_active.add(r);
            }
        }
        
        _directAssigned = RFIHelpers.getDirectRFIs(me, filteredEvent, sort.get(2), sortDirection.get(2));
        for ( RFI r:_directAssigned) {
            if ( r.isClosed()) {
                directAssigned_completed.add(r);
            }
        }
        
        if ( me.groups != null && me.groups.size() > 0) {
            _inMyGroup = RFIHelpers.getRFIsInGroups(Group.collectIds(me.groups), filteredEvent, sort.get(3), sortDirection.get(3));
            for ( RFI r:_inMyGroup) {
                if ( r.isPending()) {
                    inMyGroup_pending.add(r);
                }
            }
            
            _inMyGroup = RFIHelpers.getRFIsInGroups(Group.collectIds(me.groups), filteredEvent, sort.get(4), sortDirection.get(4));
            for ( RFI r:_inMyGroup) {
                if ( r.isActive()) {
                    inMyGroup_active.add(r);
                }
            }
            
            _inMyGroup = RFIHelpers.getRFIsInGroups(Group.collectIds(me.groups), filteredEvent, sort.get(5), sortDirection.get(5));
            for ( RFI r:_inMyGroup) {
                if ( r.isClosed()) {
                    inMyGroup_completed.add(r);
                }
            }
        }
        
        render(directAssigned_pending, directAssigned_active, directAssigned_completed,
            inMyGroup_pending, inMyGroup_active, inMyGroup_completed, filteredEvent);
    }
    
    // You guessed it: /admin/dashboard page in the admin experience
    public static void dashboard(Long event, @As(",") List<String> sort, @As(",") List<String> sortDirection, @As("yyyy-MM-dd") Date startDate, @As("yyyy-MM-dd") Date endDate, Long group, long activity) {
        AtomicReference<List<String>> _sort = new AtomicReference<List<String>>(sort);
        AtomicReference<List<String>> _sortDirection = new AtomicReference<List<String>>(sortDirection);
        ApplicationHelpers.sanitizeSortInfo(_sort, _sortDirection, 3);
        sort = _sort.get();
        sortDirection = _sortDirection.get();
        
        List<RFI> pending_rfis = new ArrayList<RFI>();
        List<RFI> active_rfis = new ArrayList<RFI>();
        List<RFI> completed_rfis = new ArrayList<RFI>();
        Event e = null;
        Date now = new Date();
        
        List<RFI> tPending = (List<RFI>)RFIHelpers.filter(sort.get(0), sortDirection.get(0), 0, Integer.MAX_VALUE, true, null, null, event, null, null, null, null, startDate, endDate, null, null, null, null, group, null, null, null, null, activity, null).get("rfis");
        List<RFI> tActive = (List<RFI>)RFIHelpers.filter(sort.get(1), sortDirection.get(1), 0, Integer.MAX_VALUE, true, null, null, event, null, null, null, null, startDate, endDate, null, null, null, null, group, null, null, null, null, activity, null).get("rfis");
        List<RFI> tCompleted = (List<RFI>)RFIHelpers.filter(sort.get(2), sortDirection.get(2), 0, Integer.MAX_VALUE, true, null, null, event, null, null, null, null, startDate, endDate, null, null, null, null, group, null, null, null, null, activity, null).get("rfis");
        
        for ( RFI r: tPending) {
            if ( r.isPending()) {
                pending_rfis.add(r);
            }
        }
        
        for ( RFI r: tActive) {
            if ( r.isActive()) {
                active_rfis.add(r);
            }
        }
        
        for ( RFI r: tCompleted) {
            if ( r.isClosed()) {
                completed_rfis.add(r);
            }
        }
        
        if ( event != null) {
            session.put("event", event);
        }
        else {
            session.remove("event");
        }
        
        renderTemplate("AdminController/dashboard.html", e, pending_rfis, active_rfis, completed_rfis, startDate, endDate);
    }
    
    // JSON route for creating an admin note (accessible from the "Notes" button the dashboard admin experience page)
    @Restrict("!Field")
    public static void createAdminNote(@Valid AdminComment c) {
        if ( validation.hasErrors()) {
            ApplicationHelpers.setResponseError(response);
            Map<String, List> errors = new HashMap<String,List>();
            errors.put("errors", ApplicationHelpers.getErrors(validation.errors()));
            serializeErrors(errors);
        }
        else {
            c.createdBy = Master.connected();
            c.save();
            serializeComments(c);
        }
    }
    
    @Restrict("!Field")
    public static void deleteAdminNote(long id) {
        Logger.info(String.format("%d", id));
        AdminComment c = AdminComment.findById(id);
        c.delete();
        serialize("OK");
    }
    
    // /admin/importexport, via "Import/Export RFIs" button available under "My RFIs" or "RFIs" pages in the admin experience
    @Restrict("!Field")
    public static void importExport() {
        render();
    }
    
    // /admin/import via POST - Step 1 of the import after specifying a JSON source file
    @Restrict("!Field")
    public static void importRFIs(File rfi_file) throws FileNotFoundException, IOException {
        validation.required("rfi_json_missing", rfi_file).message("rfi_json_missing");
        if ( validation.hasErrors()) {
            renderTemplate("AdminController/importExport.html");
        }
        else {
            ImportedFile att = new ImportedFile();
            att.attachmentFilename = rfi_file.getName();
            att.associatedFile = new Blob();
            att.associatedFile.set(new FileInputStream(rfi_file), MimeTypes.getContentType(rfi_file.getName()));
            att.createdBy = Master.connected();
            att.save();
            Long id = att.id;
            importChoices(id);
        }
    }
    
    // /admin/importchoices - Step 2 of the import, select your RFIs to import (display conflicts, etc, etc)
    @Restrict("!Field")
    public static void importChoices(Long id) throws IOException {
        ImportedFile f = ImportedFile.findById(id);
        String content = FileUtils.readFileToString(f.associatedFile.getFile());
        
        try {
            List<RFI> rfis = deserializeRFIs(content);
            List<RFI> rfi_conflicts = new ArrayList<RFI>();
            List<RFI> ok_rfis = new ArrayList<RFI>();
            for ( RFI r:rfis) {
                RFI cur = RFI.find("byUUID", r.uuid).first();
                if ( cur != null) {
                    rfi_conflicts.add(r);
                }
                else {
                    ok_rfis.add(r);
                }
            }
            
            Long fileid = f.id;
            render(rfi_conflicts, ok_rfis, fileid);
        } catch( Exception e) {
            validation.addError("rfi_json_invalid", "rfi_json_invalid");
            validation.keep();
            importExport();
        }
    }
    
    /*
        Import the RFIs in the stored JSON file into the destination database.
        
        If RFI exists in existing syste, (UUID matches), then update the RFI content
            AND attach the associated event (by name OR by UUID), event activity (by UUID)
            AND group (by UUID).  If associated event is NOT found, an event is created.
            No associated comments, attachments or associated items (weblink/product urls) are imported.
        If RFI doesn't exist, then create new RFI, associate event (again find by name OR UUID) w/ RFI.
            If associated event is not found, create a new one.
            Create comments, attachments, related items in the destination.
            Also, associate event activites (by UUID), if not found,
                then create and associated w/ RFI AND associated Event.
    */
    @Restrict("!Field")
    public static void doImport(String rfis, ImportedFile f) throws IOException {
        String content = FileUtils.readFileToString(f.associatedFile.getFile());
        String [] rfiIDs = rfis.split(",\\s+");
        Logger.debug(StringUtils.join(rfiIDs, ","));
        
        List<RFI> fileRFIs = deserializeRFIs(content);
        Logger.debug(String.format("Deserialized %d rfis", fileRFIs.size()));
        List<RFI> imports = new ArrayList<RFI>();
        List<Event> eventsCreated = new ArrayList<Event>();
        
        for (String cur : rfiIDs) {
            RFI r = null;
            // There's gotta be a way to do this using a predicate...
            Logger.debug(String.format("Deserialized %d rfis", fileRFIs.size()));
            for ( RFI b:fileRFIs) {
                Logger.debug(String.format("RFI FROM FILE: %s", b));
                if ( b.id.equals(new Long(cur))) {
                    r = b;
                }
            }
            
            if ( r != null) {
                fileRFIs.remove(r);
                // Check the DB to see if it already exists...
                RFI dbRFI = RFI.find("byUUID", r.uuid).first();
                if ( dbRFI != null) {
                    // RFI already exists, overwrite it
                    Logger.info("Already found an RFI w/ UUID %s", r.uuid);
                    Logger.info("Associated event: %s", r.event.toString());
                    RFI toUpdate = RFI.find("byUUID", r.uuid).first();
                    RFIHelpers.copyLeft(toUpdate, r);
                    Event destEvent = Event.fuzzyFind(r.event);
                    if ( destEvent == null) {
                        r.event.id = null;
                        r.event.save();
                        eventsCreated.add(r.event);
                        destEvent = r.event;
                    }
                    
                    if ( r.eventActivity != null) {
                        r.eventActivity = EventActivity.find("byUUID", r.eventActivity.uuid).first();
                    }
                    
                    if ( r.group != null) {
                        r.group = Group.find("byUUID", r.group.uuid).first();
                    }
                    
                    toUpdate.event = destEvent;
                    toUpdate.save();
                }
                else {
                    // Just create a new RFI...argh, this crap *should* be cascading the save when
                    // setting the comment id to null...not sure why we have to do this manually
                    r.id = null;
                    List<Comment> comments = r.comments;
                    List<AdminComment> internalComments = r.internalComments;
                    List<Attachment> attachments = r.attachments;
                    List<RelatedRFIItem> related = r.relatedItems;
                    Event destEvent = Event.fuzzyFind(r.event);
                    Logger.debug("DESTEVENT: %s", destEvent);
                    if ( destEvent == null) {
                        r.event.id = null;
                        r.event.save();
                        eventsCreated.add(r.event);
                        destEvent = r.event;
                    }
                    
                    r.event = destEvent;
                    
                    if ( r.group != null) {
                        r.group = Group.find("byUUID", r.group.uuid).first();
                    }
                    
                    if ( r.eventActivity != null) {
                        EventActivity act = EventActivity.find("byUUID", r.eventActivity.uuid).first();
                        if ( act == null) {
                            // Create new activity if it doesn't exist in the destination system
                            r.eventActivity.id = null;
                            r.eventActivity.save();
                            r.event.eventActivities.add(r.eventActivity);
                            r.event.save();
                        }
                        else {
                            r.eventActivity = act;
                        }
                    }
                    
                    r.comments = new ArrayList<Comment>();
                    r.attachments = new ArrayList<Attachment>();
                    r.relatedItems = new ArrayList<RelatedRFIItem>();
                    r.internalComments = new ArrayList<AdminComment>();
                    r.save();
                    
                    for ( Comment c:comments) {
                        c.id = null;
                        c.rfi = r;
                        c.save();
                    }
                    
                    for ( RelatedRFIItem rel:related) {
                        rel.id = null;
                        rel.rfi = r;
                        rel.save();
                    }
                    
                    for ( Attachment a:attachments) {
                        a.id = null;
                        a.rfi = r;
                        a.save();
                    }
                    
                    for ( AdminComment c:internalComments) {
                        c.id = null;
                        c.rfi = r;
                        c.save();
                    }
                }
                
                imports.add(r);
            }
        }
        
        if ( imports.size() > 0) {
            flash.now("success", String.format("%d RFIs imported, %d events created", imports.size(), eventsCreated.size()));
        }
        render(imports);
    }
    
    // /admin/export via POST - dump RFIs to JSON (via selected IDs)
    @Restrict("!Field")
    public static void exportRFIs(List<Long> ids) {
        validation.required("export_ids", ids);
        if (validation.hasErrors()) {
            validation.keep();
            importExport();
        }
            
        List<RFI> rfis = RFI.getByIDs(ids);
        
        DateFormat dateFormat = new SimpleDateFormat(ConfigurationHelpers.getExportDateFormat());
        Date date = new Date();
        String timeStamp = dateFormat.format(date);
        response.contentType = "application/json";
        String ret = SerializationHelpers.getRFIFullSerializer().serialize(rfis);
        renderBinary(new ByteArrayInputStream(ret.getBytes()), String.format("RFIExport-%s.json", timeStamp));
    }
    
    
    // /admin/events - "Events" in main nav of admin experience
    @Restrict("!Field")
    public static void events(Boolean active, int page, int pageSize, String sort, String sortDirection) {
        pageSize = ApplicationHelpers.sanitizePageSize(session, pageSize);
        Map<String,Object> toParse = Event.grid(sort,sortDirection,page,pageSize,active);
        page = (Integer)toParse.get("page");
        pageSize = (Integer)toParse.get("pageSize");
        Long totalCount = (Long)toParse.get("totalCount");
        int totalPages = (Integer)toParse.get("totalPages");
        sort = (String)toParse.get("sort");
        sortDirection = (String)toParse.get("sortDirection");
        List<Event> events = (List<Event>)toParse.get("events");
        active = (Boolean)toParse.get("active");
        
        render(events, pageSize, totalCount, page, sortDirection, active, totalPages);
    }
    
    // /admin/users - "Users" in the main nav of the admin experience
    @Restrict("Management")
    public static void users(int page, int pageSize, String sort, String sortDirection, String search) {
        pageSize = ApplicationHelpers.sanitizePageSize(session, pageSize);
        Map<String,Object> toParse = User.grid(sort,sortDirection,page,pageSize, search);
        page = (Integer)toParse.get("page");
        pageSize = (Integer)toParse.get("pageSize");
        Long totalCount = (Long)toParse.get("totalCount");
        int totalPages = (Integer)toParse.get("totalPages");
        sort = (String)toParse.get("sort");
        sortDirection = (String)toParse.get("sortDirection");
        List<User> users = (List<User>)toParse.get("users");
        
        render(users, totalCount, totalPages, page, pageSize, sort, sortDirection);
    }
    
    // /admin/editEvent - Edit an event in the admin experience
    @Restrict("!Field")
    public static void editEvent(Long id, String ret) {
        Event e = Event.findById(id);
        if ( e == null) {
            notFound("Event not found");
        }
        render(e, ret);
    }
    
    // /admin/editUser - Edit a user in the admin experience
    @Restrict("Management")
    public static void editUser(Long id, String ret) {
        User u = User.findById(id);
        if ( u == null) {
            notFound("User not found");
        }
        render(u, ret);
    }
    
    // /admin/editMyInfo - Abridge user edit form only for analysts
    public static void editMyInfo(String ret) {
        User u = Master.getUser();
        render(u, ret);
    }
    
    /*
        @As("profile") only allows a subset of info to be updated in the POST:
        eg. "Analyst" user cannot update their role
    */
    public static void updateMyInfo(@As("profile") User u, String ret) {
        User me = Master.getUser();
        if ( u.id != me.id) {
            flash.error("Cannot update someone else's information");
            u = me;
            renderTemplate("AdminController/editMyInfo.html", u, ret);
        }
        else {
            Pattern p = Pattern.compile("(\\w+),?$");
            Matcher m = p.matcher(u.agency.trim());
            
            if ( m.find()) {
                String org = m.group(1);
                u.agency = org.trim();
            }
            else {
                u.agency = "";
            }
            
            validateUser(u);
            if ( validation.hasErrors()) {
                renderTemplate("AdminController/editMyInfo.html", u, ret);
            }
            else {
                flash.success("User \"%s\" updated", u.userName);
                u.save();
                Activity.LogUserActivity(u, Master.connected(), ActivityType.CONTENT_UPDATED);
                if ( ret != null && !ret.equals("")) {
                    redirect(ret);
                }
                else {
                    index(null);
                }
            }
        }
    }
    
    @Restrict("!Field")
    public static void updateEvent(Event e, String ret, Boolean createGeoQProject, EventActivity newActivity) {
        Event.em().detach(e);
        validateEvent(e);
        Event old = e.id == null ? null : (Event)Event.findById(e.id);
        Event.em().detach(old);
        Boolean isNew = e.id == null ? true: false;
        if ( validation.hasErrors()) {
            renderTemplate("AdminController/editEvent.html", isNew, e, ret, createGeoQProject, newActivity);
        }
        else {
            e = Event.em().merge(e);
            e.save();
            
            if ( newActivity != null && newActivity.name != null && !newActivity.name.trim().equals("")) {
                newActivity.save();
                e.eventActivities.add(newActivity);
                e.save();
            }
            
            if ( createGeoQProject != null && createGeoQProject && (e.geoQId == null || e.geoQId.equals(""))) {
                GEOQIntegration.putGeoQProject(e);
            }
            
            if ( isNew) {
                e.createdBy = Master.connected();
                EventHelpers.handleEventCreated(e, Master.connected());
                Activity.LogEventActivity(e, Master.connected(), ActivityType.CREATED);
                flash.success("Event \"%s\" created", e.name);
            }
            else {
                EventHelpers.handleEventUpdated(e, Master.connected(), old);
                Activity.LogEventActivity(e, Master.connected(), ActivityType.CONTENT_UPDATED);
                flash.success("Event \"%s\" updated", e.name);
            }
            if ( ret != null && !ret.equals("")) {
                redirect(ret);
            }
            else {
                events(null, 0, 0, null, null);
            }
        }
    }
    
    @Restrict("Management")
    public static void deleteGroup(long id) {
        Group cur = Group.findById(id);
        if ( cur != null) {
            List<User> users = User.find("byGroup", cur).fetch();
            for ( User u:users) {
                u.groups = null;
                u.save();
            }
            List<Group> groups = Group.find("byParent", cur).fetch();
            for ( Group g:groups) {
                g.parent = null;
                g.save();
            }
            List<Event> events = Event.find("byGroup", cur).fetch();
            for ( Event e:events) {
                e.groups = null;
                e.save();
            }
            List<RFI> rfis = RFI.find("byGroup", cur).fetch();
            for ( RFI r:rfis) {
                r.group = null;
                r.save();
            }
            cur.delete();
        }
        renderText("Deleted");
    }
    
    // /admin/groups - via "Groups" in the main nav of the admin experience
    @Restrict("Management")
    public static void groups(int page, int pageSize, String sort, String sortDirection) {
        pageSize = ApplicationHelpers.sanitizePageSize(session, pageSize);
        Map<String,Object> toParse = Group.grid(sort,sortDirection,page,pageSize);
        page = (Integer)toParse.get("page");
        pageSize = (Integer)toParse.get("pageSize");
        Long totalCount = (Long)toParse.get("totalCount");
        int totalPages = (Integer)toParse.get("totalPages");
        sort = (String)toParse.get("sort");
        sortDirection = (String)toParse.get("sortDirection");
        List<Group> groups = (List<Group>)toParse.get("groups");
        render(groups, totalCount, totalPages, page, pageSize, sort, sortDirection);
    }
    
    @Restrict("Management")
    public static void groupsJSON() {
        serializeGroups(Group.all().fetch());
    }
    
    // /admin/editGroup - Edit a group in the admin experience
    @Restrict("Management")
    public static void editGroup(Long id, String ret) {
        Group g = Group.findById(id);
        render(g, ret);
    }
    
    @Restrict("Management")
    public static void updateGroup(@Valid Group g, String ret) {
        boolean isNew = g.id == null ? true:false;
        if (validation.hasErrors()) {
            renderTemplate("AdminController/editGroup.html", g, ret);
        }
        else {
            flash.success("Group \"%s\" updated", g.name);
            if ( isNew) {
                g.createdBy = Master.connected();
            }
            g.save();
            
            if ( ret != null && !ret.equals("")) {
                redirect(ret);
            }
            else {
                groups(0, 0, null, null);
            }
        }
    }
    
    @Restrict("Management")
    public static void updateUser(User u, String ret) {
        Pattern p = Pattern.compile("(\\w+),?$");
        Matcher m = p.matcher(u.agency.trim());
        
        if ( m.find()) {
            String org = m.group(1);
            u.agency = org.trim();
        }
        else {
            u.agency = "";
        }
        
        validateUser(u);
        if ( validation.hasErrors()) {
            renderTemplate("AdminController/editUser.html", u, ret);
        }
        else {
            flash.success("User \"%s\" updated", u.userName);
            u.save();
            Activity.LogUserActivity(u, Master.connected(), ActivityType.CONTENT_UPDATED);
            if ( ret != null && !ret.equals("")) {
                redirect(ret);
            }
            else {
                users(0,0,null,null, null);
            }
        }
    }
    
    // /admin/edit - Edit an RFI in the admin experience
    @Restrict("!Field")
    public static void edit(Long id, String ret) {
        List<Country> countries = Country.getAll();
        RFI r = RFI.findById(id);
        if ( r == null) {
            notFound("RFI not found");
        }
        List<Event> events = Event.getAll();
        List<SelectContainer> states = ConfigurationHelpers.getStates();
        renderTemplate("AdminController/editRFI.html", countries, r, events, states, ret);
    }
    
    //TODO: Barf, look into commons for list comprehensions...also, these should be
    // placed in a helper or something closer to the RFI model
    static Long countOverdue(List<RFI> rfis) {
        Long cnt = 0L;
        for ( RFI r:rfis) {
            if ( r.isOverdue()) {
                cnt++;
            }
        }
        return cnt;
    }
    
    static Long countActive(List<RFI> rfis) {
        Long cnt = 0L;
        for ( RFI r:rfis) {
            if ( r.isActive()) {
                cnt++;
            }
        }
        return cnt;
    }
    
    static Long countClosed(List<RFI> rfis) {
        Long cnt = 0L;
        for ( RFI r:rfis) {
            if ( r.isClosed()) {
                cnt++;
            }
        }
        return cnt;
    }
    
    // Update an RFI
    public static void update(RFI r, String oldStatus, String oldAssigned, String ret, String oldGroupId, boolean createGEOQJob) {
        RFIHelpers.validateRFI(r, validation);
        Boolean isNew = r.id == null ? true: false;
        Logger.debug("CREATE GEOQ JOB? %s", createGEOQJob);
        if ( validation.hasErrors()) {
            List<Country> countries = Country.getAll();
            List<Event> events = Event.getAll();
            List<SelectContainer> states = ConfigurationHelpers.getStates();
            renderTemplate("AdminController/editRFI.html", isNew, countries, r, events, states, oldStatus, ret, oldGroupId, oldAssigned, createGEOQJob);
        }
        else {
            if ( r.nonSpatial) {
                r.region = "";
                r.polyline = "";
                r.coordinates.lat = 0;
                r.coordinates.lon = 0;
            }
            
            if ( isNew) {
                r.createdBy = Master.connected();
                flash.success("RFI \"%s\" created", r.title);
            }
            else {
                flash.success("RFI \"%s\" updated", r.title);
            }
            r.save();
            
            if ( isNew) {
                RFIHelpers.handleRFICreated(r, Master.connected());
            }
            else {
                RFIHelpers.handleRFIUpdated(r, oldAssigned, oldStatus, Master.connected(), oldGroupId);
            }
            
            if ( createGEOQJob) {
                GEOQIntegration.putGeoQJob(r);
            }
            
            if ( ret != null && !ret.equals("")) {
                redirect(ret);
            }
            else {
                //FML
                rfis(true, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 0L, null);
            }
        }
        renderTemplate("AdminController/editRFI.html", r, oldStatus, oldAssigned, ret, oldGroupId, createGEOQJob);
    } 
    
    //TODO: I don't think we need this, just call "validation.valid()"
    static void validateUser(User u) {
        validation.valid(u);
    }
    
    //TODO: I don't think we need this, just call "validation.valid()"
    static void validateEvent(Event e) {
        validation.valid(e);
    }
    
    // /admin/newGroup
    @Restrict("Management")
    public static void newGroup() {
        Boolean isNew = true;
        renderTemplate("AdminController/editGroup.html", isNew);
    }
    
    // /admin/newEvent
    @Restrict("!Field")
    public static void newEvent() {
        Boolean isNew = true;
        renderTemplate("AdminController/editEvent.html", isNew);
    }
    
    // /admin/new - New RFI
    public static void newRFI() {
        Boolean isNew = true;
        List<Country> countries = Country.getAll();
        List<Event> events = Event.getAllNotArchived();
        List<SelectContainer> states = ConfigurationHelpers.getStates();
        renderTemplate("AdminController/editRFI.html", isNew, countries, events, states);
    }
    
    static List<RFI> deserializeRFIs(String content) {
        List<RFI> fileRFIs = (List<RFI>)SerializationHelpers.getRFIFullDeserializer().deserialize(content);
        return fileRFIs;
    }
}
