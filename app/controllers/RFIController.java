package controllers;

import helpers.*;

import play.*;
import play.data.parsing.*;
import play.libs.*;
import play.mvc.*;
import play.db.jpa.*;
import play.data.*;
import play.data.binding.*;
import play.data.validation.*;

import java.util.*;
import java.io.*;
import java.text.*;
import java.lang.reflect.*;
import java.util.concurrent.atomic.*;

import models.*;

import flexjson.*;

import controllers.deadbolt.*;

import javax.persistence.*;
import javax.persistence.criteria.*;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;

import org.apache.commons.io.IOUtils;

// The controll that handles RFI editing on the map experience
@With({Master.class, Deadbolt.class})
public class RFIController extends BaseController {
    // Gridview of rfis on the map experience (available via "Overviews -> Show RFI List" or via "RFI List" button in main nav
    public static void grid(String sort, String sortDirection,
            String filterRequestorFirstName, String filterRequestorLastName,
            long filterEvent, String filterOrganization,
            String filterAssignee, String filterSearch, String filterTitle,
            @As("yyy-MM-dd") Date startDate, @As("yyyy-MM-dd") Date endDate,
            String filterInstructions, String filterCity, String filterComment, String filterID,
            String filterCountry, String filterBE, String filterStatus, long filterActivity, String filterProduct
    ) {
        if ( sort == null) {
            sort = "id";
        }
        if ( sortDirection == null) {
            sortDirection = "desc";
        }
        
        String _params = String.format("sort=%s&sortDirection=%s",
            sort, sortDirection);
        
        Map<String, Object> ret = RFIHelpers.filter(sort, sortDirection, 0, Integer.MAX_VALUE, true,
            filterRequestorFirstName, filterRequestorLastName, filterEvent,
            filterOrganization, filterAssignee, filterSearch,
            filterTitle, startDate, endDate, filterInstructions, filterCity,
            filterComment, filterID, null, null, filterCountry, filterBE, filterStatus, filterActivity, filterProduct);
        List<RFI> rfis = (List<RFI>)ret.get("rfis");
        long totalCount = ((List<RFI>)ret.get("rfis")).size();
        
        render(rfis, totalCount, _params);
    }

    // HTML route handlers
    public static void create(RFI r) {
        _create(Router.reverse("Application.index", new HashMap()).url, "RFIController/newRFI.html", r, false, false);
    }
    
    // "My RFIs" grid, available via "Overviews -> My RFIs"
    public static void myGrid(long filterEvent, @As(",") List<String> sort, @As(",") List<String> sortDirection) {
        User me = Master.getUser();
        List<RFI> directAssigned = new ArrayList<RFI>();
        List<RFI> inMyGroup = new ArrayList<RFI>();
        
        Logger.debug("SORT: %s", sort);
        Logger.debug("SORTDIRECTION: %s", sortDirection);
        
        AtomicReference<List<String>> _sort = new AtomicReference<List<String>>(sort);
        AtomicReference<List<String>> _sortDirection = new AtomicReference<List<String>>(sortDirection);
        
        ApplicationHelpers.sanitizeSortInfo(_sort, _sortDirection, 2);
        
        sort = _sort.get();
        sortDirection = _sortDirection.get();
        
        Logger.debug("SORT2: %s", sort);
        Logger.debug("SORTDIRECTION2: %s", sortDirection);
        
        String _params = String.format("sort=%s&sortDirection=%s",
            StringUtils.join(sort, "%2C"), StringUtils.join(sortDirection, "%2C"));

        Event filteredEvent = Event.findById(filterEvent);
        
        if ( me.groups != null && me.groups.size() > 0) {
            inMyGroup = RFIHelpers.getRFIsInGroups(Group.collectIds(me.groups), filteredEvent, sort.get(1), sortDirection.get(1));
        }
        directAssigned = RFIHelpers.getDirectRFIs(me, filteredEvent, sort.get(0), sortDirection.get(0));
        
        render(directAssigned, inMyGroup, filteredEvent, _params);
    }
    
    // New RFI dialog content - available via a number of ways
    public static void newRFI() {
        List<Country> countries = Country.getAll();
        List<Event> events = Event.getAllNotArchived();
        List<SelectContainer> states = ConfigurationHelpers.getStates();
        render(countries, events, states);
    }
    
    // Edit RFI - same dialog as "New RFI", just slightly different content
    public static void edit(Long id) {
        List<Country> countries = Country.getAll();
        RFI r = RFI.findById(id);
        List<Event> events = Event.getAllNotArchived();
        List<SelectContainer> states = ConfigurationHelpers.getStates();
        render(countries, r, events, states);
    }
    
    public static void update(RFI r, String oldStatus, String oldAssigned, String oldGroupId, boolean createGEOQJob) {
        _update("RFIController/edit.html", r, oldStatus, false, oldAssigned, oldGroupId, createGEOQJob);
    }
    
    @Restrict("!Field")
    // When deleting an RFI via map experience (available via a few methods, including via "Actions" button on "RFI List" on an individual RFI
    public static void delete(long id) {
        RFI r = RFI.findById(id);
        RFIHelpers.handleRFIDeleted(r);
        r.delete();
    }
    
    @Restrict("!Field")
    // When verifying an RFI via map experience (available via "Actions" button on "RFI List" on an individual RFI)
    public static void verify(long id) {
        RFI r = RFI.findById(id);
        if ( r != null) {
            r.status = ApplicationHelpers.ACCEPTED;
            r.save();
            RFIHelpers.handleRFIUpdated(r, "", ApplicationHelpers.PENDING, Master.connected(), "");
        }
    }
    
    // Not currently used since the attachments feature of the RFI Generator is turned off
    public static void renderAttachment(long id) {
        Attachment attachment = Attachment.findById(id);
        response.contentType = "application/octet-stream";
        response.setHeader("Cache-Control", "");
        renderBinary(new ByteArrayInputStream(Base64.decodeBase64(attachment.fileContent)), attachment.attachmentFilename);
    }
    
    @Restrict("!Field")
    // Available via admin experience when editing an RFI, under "Status (Admin-only)", JSON
    public static void createAdminComment(@Valid AdminComment c) {
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
    public static void deleteAdminComment(long id) {
        AdminComment c = AdminComment.findById(id);
        c.delete();
        serialize("OK");
    }
    
    public static void createComment(@Valid Comment c) {
        if ( validation.hasErrors()) {
            ApplicationHelpers.setResponseError(response);
            RFI rfi = c.rfi;
            renderTemplate("RFIController/comment.html", rfi);
        }
        else {
            c.createdBy = Master.connected();
            c.save();
            RFIHelpers.handleCommentCreated(c);
            flash.success("Comment created");
        }
    }
    
    // Create an end-user comment via JSON (used in the admin experience when editing an RFI under "User comments")
    public static void createCommentJSON(@Valid Comment c) {
        if ( validation.hasErrors()) {
            ApplicationHelpers.setResponseError(response);
            RFI rfi = c.rfi;
            Map<String, List> errors = new HashMap<String,List>();
            errors.put("errors", ApplicationHelpers.getErrors(validation.errors()));
            serializeErrors(errors);
        }
        else {
            c.createdBy = Master.connected();
            c.save();
            RFIHelpers.handleCommentCreated(c);
            serializeComments(c);
        }
    }
    
    // Available via map experience via "View/Comment" button when clicking on an RFI
    public static void comment(long id) {
        RFI rfi = RFI.findById(id);
        render(rfi);
    }
    
    
    // Not used as file attachments are currently disabled
    public static void upload() throws FileNotFoundException, IOException {
        if ( ConfigurationHelpers.attachmentsEnabled()) {
            response.contentType = "text/html";
            Logger.debug("%s", request.headers.get("x-requested-with"));
            if ( request.headers.containsKey("x-requested-with")) {
                // Using XMLHttpRequest
                byte [] body = IOUtils.toByteArray(request.body);
                
                Attachment att = new Attachment();
                Long id = request.params.get("rfi_id", Long.class);
                att.rfi = RFI.findById(id);
                String qqfile = request.params.get("qqfile", String.class);
                att.attachmentFilename = qqfile;
                att.fileContent = Base64.encodeBase64String(body);
                att.createdBy = Master.connected();
                att.save();
                
                Activity.LogRFIActivity(att.rfi, Master.connected(), ActivityType.UPLOAD_ADDED);
                
                renderText("{success: true}");
            }
            else {
                DataParser parser = DataParser.parsers.get(request.contentType);
                parser.parse(request.body);
                Attachment att = new Attachment();
                Long id = request.params.get("rfi_id", Long.class);
                att.rfi = RFI.findById(id);
                ArrayList<FileUpload> uploads = (ArrayList<FileUpload>)request.args.get("__UPLOADS");
                for ( FileUpload fu:uploads) {
                    if ( "qqfile".equals(fu.getFieldName())) {
                        att.attachmentFilename = fu.getFileName();
                        att.fileContent = Base64.encodeBase64String(IOUtils.toByteArray(fu.asStream()));
                        att.createdBy = Master.connected();
                        att.save();
                    }
                }
                
                Activity.LogRFIActivity(att.rfi, Master.connected(), ActivityType.UPLOAD_ADDED);
                renderText("{success: true}");
            }
        }
        
        renderText("{success: false}");
    }
    
    @Restrict("!Field")
    // Export to JSON from w/in the admin experience available via "Import/export" under "RFI List" the "By Event" submit button
    public static void exportRFIsByEvent(long event) {
        List<RFI> rfis = null;
        if ( event != 0) {
            rfis = RFI.find("byEvent", Event.findById(event)).fetch();
        }
        else {
            rfis = RFI.all().fetch();
        }
        
        DateFormat dateFormat = new SimpleDateFormat(ConfigurationHelpers.getExportDateFormat());
        Date date = new Date();
        String timeStamp = dateFormat.format(date);
        response.contentType = "application/json";
        String ret = SerializationHelpers.getRFIFullSerializer().serialize(rfis);
        response.setHeader("Pragma", "public");
        response.setHeader("Cache-Control", "cache, must-revalidate");
        renderBinary(new ByteArrayInputStream(ret.getBytes()), String.format("RFIExport-%s.json", timeStamp));
    }
    
    //TODO: Rewrite to accept value type vs reference type
    @Restrict("!Field")
    // Exports RFI to JSON, accessible from w/in the admin experience when editing an RFI ("Export RFI" button)
    public static void exportSingle(Long id) {
        List<RFI> rfis = RFI.find("id = ?", id).fetch();
        if ( rfis.size() > 0) {
            response.contentType = "application/json";
            DateFormat dateFormat = new SimpleDateFormat(ConfigurationHelpers.getExportDateFormat());
            Date date = new Date();
            String timeStamp = dateFormat.format(date);
            String ret = SerializationHelpers.getRFIFullSerializer().serialize(rfis);
            response.setHeader("Pragma", "public");
            response.setHeader("Cache-Control", "cache, must-revalidate");
            renderBinary(new ByteArrayInputStream(ret.getBytes()), String.format("RFIExport-%s-%s.json", rfis.get(0).networkID, timeStamp));
        }
        else {
            notFound();
        }
    }
    
    @Restrict("!Field")
    // Export to XLS on the map/admin experiences (available via "RFI List" on the map experience or "Import/Export RFIs" button in the admin experience)
    public static void exportXLS(String sort, String sortDirection, String filterRequestorFirstName,
        String filterRequestorLastName, Long filterEvent, String filterOrganization, String filterAssignee,
        String filterSearch, String filterTitle, @As("yyyy-MM-dd") Date startDate, @As("yyyy-MM-dd") Date endDate,
        String filterInstructions, String filterCity, String filterComment, String filterID,
        String filterCountry, String filterBE, String filterStatus, long filterActivity, String filterProduct
    ) throws IOException, ClassNotFoundException, IllegalAccessException, InvocationTargetException, ParseException, NoSuchMethodException {
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        Map<String, Object> ret = RFIHelpers.filter(sort, sortDirection, 0, Integer.MAX_VALUE, true,
            filterRequestorFirstName, filterRequestorLastName,
            filterEvent, filterOrganization, filterAssignee, filterSearch, filterTitle,
            startDate, endDate, filterInstructions, filterCity, filterComment, filterID,
            null, null, filterCountry, filterBE, filterStatus, filterActivity, filterProduct);
        List<RFI> rfis = (List<RFI>)ret.get("rfis");
    
        ByteArrayInputStream inp = RFIHelpers.exportToXLS(out, rfis);
        
        DateFormat dateFormat = new SimpleDateFormat(ConfigurationHelpers.getExportDateFormat());
        Date now = new Date();
        String timeStamp = dateFormat.format(now);
        
        response.setHeader("Pragma", "public");
        response.setHeader("Cache-Control", "cache, must-revalidate");
        response.setHeader("Content-type", "application/vnd.ms-excel");
        renderBinary(inp, String.format("RFIWorkbook-%s.xls", timeStamp));
    }
    
    // JSON routes
    public static void createJSON(String body) {
        RFI r = getFromJSON(body);
        _create(null, null, r, true, false);
    }
    
    @Restrict("!Field")
    // JSON route for adding a weblink to an RFI (available via both admin/map experience on the RFI edit form)
    public static void addLink(@Valid RelatedRFIItem rel) {
        if ( validation.hasErrors()) {
            Map<String, List> errors = new HashMap<String,List>();
            errors.put("errors", ApplicationHelpers.getErrors(validation.errors()));
            ApplicationHelpers.setResponseError(response);
            serializeErrors(errors);
        }
        else {
            rel.itemType = "WEBLINK";
            rel.createdBy = Master.connected();
            rel.save();
            
            serializeRelatedItems(rel);
        }
    }
    
    @Restrict("!Field")
    // JSON route for adding a product URL to an RFI (available via both admin/experience on the RFI edit form)
    public static void addProduct(@Valid RelatedRFIItem rel) {
        if ( validation.hasErrors()) {
            Map<String, List> errors = new HashMap<String,List>();
            errors.put("errors", ApplicationHelpers.getErrors(validation.errors()));
            ApplicationHelpers.setResponseError(response);
            serializeErrors(errors);
        }
        else {
            rel.itemType = "PRODUCT";
            rel.createdBy = Master.connected();
            rel.save();
            
            RFIHelpers.handleRFIProductAdded(rel);
            
            serializeRelatedItems(rel);
        }
    }
    
    static void updateJSON(String body) {
        RFI r = getFromJSON(body);
        RFI or = RFI.findById(r.id);
        String oldStatus = or.status;
        String oldAssigned = or.assignedTo;
        String oldGroupId = or.group == null ? "":or.group.id.toString();
        RFI ret = RFIHelpers.copyLeft(or, r);
        
        _update(null, ret, oldStatus, true, oldAssigned, oldGroupId, false);
    }
    
    // /rfis - JSON route for getting a list of RFIS, can include attachments serialized as base64-encoded text
    public static void getAll(Long event, Boolean attachments, boolean hideArchived) {
        List<RFI> rfis;
        User usr = Master.getUser();
        
        Logger.debug("%s", hideArchived);
        
        String notArchived = " and (event.archived is null or event.archived = 0) and (archived is null or archived = 0)";
        if ( event != null) {
            String qry = "event.id = ?";
            if ( hideArchived == true) {
                qry += notArchived;
            }
            rfis = RFI.find(qry, event).fetch();
        }
        else {
            String qry = "1 = 1";
            if ( hideArchived == true) {
                qry += notArchived;
            }
            rfis = RFI.find(qry).fetch();
        }
        if ( attachments != null && attachments) {
            serializeFullRFIs(rfis);
        }
        else {
            serializeRFIs(rfis);
        }
    }
    
    // /rfi/[ID].json - JSON route for grabbing a single RFI by id
    public static void get(long id) {
        serializeRFIs(RFI.findById(id));
    }
    
    // Helpers...feeble attempt at refactoring
    
    static void _create(String root, String template, RFI r, Boolean json, boolean createGEOQJob) {
        RFIHelpers.validateRFI(r, validation);
        if ( validation.hasErrors()) {
            ApplicationHelpers.setResponseError(response);
            List<Country> countries = Country.getAll();
            List<Event> events = Event.getAll();
            List<SelectContainer> states = ConfigurationHelpers.getStates();
            
            if ( json) {
                Map<String, List> errors = new HashMap<String,List>();
                errors.put("errors", ApplicationHelpers.getErrors(validation.errors()));
                serializeErrors(errors);
            }
            else {
                renderTemplate(template, countries, events, r, states);
            }
        }
        else {
            if ( r.assignedTo == null) {
                r.assignedTo = "";
            }
            r.createdBy = Master.connected();
            r.save();
            
            RFIHelpers.handleRFICreated(r, Master.connected());
            
            if ( createGEOQJob) {
                GEOQIntegration.putGeoQJob(r);
            }
            
            if ( json) {
                serializeRFIs(r);
            }
            else {
                flash.success("RFI created");
            }
        }
    }

    static void _update(String template, RFI r, String oldStatus, Boolean json, String oldAssigned, String oldGroupId, boolean createGEOQJob) { 
        RFIHelpers.validateRFI(r, validation);
        List<Country> countries = Country.getAll();
        List<Event> events = Event.getAll();
        List<SelectContainer> states = ConfigurationHelpers.getStates();
        if ( validation.hasErrors()) {
            ApplicationHelpers.setResponseError(response);
            if ( json) {
                Map<String, List> errors = new HashMap<String, List>();
                errors.put("errors", ApplicationHelpers.getErrors(validation.errors()));
                serializeErrors(errors);
            }
            else {
                renderTemplate(template, countries, params, events, r, states, oldStatus, oldAssigned, oldGroupId);
            }
        }
        else {
            flash.now("success", "RFI updated");
            
            r.save();
            
            RFIHelpers.handleRFIUpdated(r, oldAssigned, oldStatus, Master.connected(), oldGroupId);
            
            if ( createGEOQJob) {
                GEOQIntegration.putGeoQJob(r);
            }
            
            if ( json) {
                serializeRFIs(r);
            }
            else {
                renderTemplate(template, countries, params, events, r, states, oldStatus, oldAssigned, oldGroupId);
            }
        }
    }

    static RFI getFromJSON(String body) {
        RFI r = new JSONDeserializer<RFI>().use(null, RFI.class)
            .use("createdAt", new IgnoreObjectFactory())
            .use("event", Event.class)
            .use("comments", new IgnoreObjectFactory())
            .use("coordinates", Point.class)
            .deserialize(body);
        r.event = Event.findById(r.event.id);
        return r;
    }
}
