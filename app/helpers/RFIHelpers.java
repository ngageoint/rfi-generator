package helpers;

import models.*;

import play.*;
import play.data.validation.*;
import play.db.jpa.*;

import java.util.regex.*;
import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import java.text.*;
import java.lang.*;

import javax.persistence.*;
import javax.persistence.criteria.*;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

// Lots of helpers associated w/ queries RFIs (for display in a sorted/paged grid) +
// other handy functions.
public class RFIHelpers {
    public static int PAGE_SIZE = Integer.MAX_VALUE;
    
    public static RFI copyLeft(RFI or, RFI r) {
        or.title = r.title;
        or.instructions = r.instructions;
        or.dateRequested = r.dateRequested;
        or.dateRequired = r.dateRequired;
        or.dateCompleted = r.dateCompleted;
        or.requestorFirstName = r.requestorFirstName;
        or.requestorLastName = r.requestorLastName;
        or.emailAddress = r.emailAddress;
        or.organization = r.organization;
        or.phoneOpen = r.phoneOpen;
        or.phoneSecure = r.phoneSecure;
        or.address1 = r.address1;
        or.address2 = r.address2;
        or.cityName = r.cityName;
        or.state = r.state;
        or.country = r.country;
        or.zipcode = r.zipcode;
        or.coordinates = r.coordinates;
        or.region = r.region;
        or.polyline = r.polyline;
        or.BENumber = r.BENumber;
        or.classification = r.classification;
        or.uuid = r.uuid;
        or.sendEmailToRequestor = r.sendEmailToRequestor;
        or.status = r.status;
        or.assignedTo = r.assignedTo;
        or.archived = r.archived;
        or.receivedVia = r.receivedVia;
        or.taskNumber = r.taskNumber;
        or.assignedPhoneOpen = r.assignedPhoneOpen;
        or.assignedPhoneSecure = r.assignedPhoneSecure;
        or.workHours = r.workHours;
        or.smtsKeywords = r.smtsKeywords;
        or.networkID = r.networkID;
        or.productFormat = r.productFormat;
        
        return or;
    }
    
    // Called when an RFI is deleted
    public static void handleRFIDeleted(RFI r) {
        if ( ConfigurationHelpers.ogcPublishingEnabled()) {
            try {
                OGCPublishing.deleteRFI(r);
            } catch (Exception ex) {
                Logger.error(ex, "Error: OGC publishing - delete");
            }
        }
    }
    
    // Called when an RFI is created
    public static void handleRFICreated(RFI r, String username) {
        Activity.LogRFIActivity(r, username, ActivityType.CREATED);

        Set<String> assigned = new HashSet<String>();
        // Send assignment notifications
        if ( !StringUtils.isBlank(r.assignedTo)) {
            if ( r.status.equals(ApplicationHelpers.PENDING)) {
                r.status = ApplicationHelpers.ACCEPTED;
                r.save();
                Activity.LogRFIActivity(r, username, ActivityType.VERIFIED);
            }
            Activity.LogRFIActivity(r, username, ActivityType.ASSIGNED);
            if ( r.sendEmailToRequestor != null && r.sendEmailToRequestor) {
                String [] emailList = r.emailAddress.toLowerCase().split(ApplicationHelpers.mailSplit);
                assigned.addAll(Arrays.asList(emailList));
            }
            
            String [] adminEmails = r.event.adminEmailAddresses.toLowerCase().split(ApplicationHelpers.mailSplit);
            assigned.addAll(Arrays.asList(adminEmails));
            
            String [] assignedToEmails = r.assignedTo.toLowerCase().split(ApplicationHelpers.mailSplit);
            assigned.addAll(Arrays.asList(assignedToEmails));
        }

        Set<String> created = new HashSet<String>();
        // Send CC to requestor(s)
        if ( r.sendEmailToRequestor != null && r.sendEmailToRequestor) {
            String [] emailList2 = r.emailAddress.toLowerCase().split(ApplicationHelpers.mailSplit);
            created.addAll(Arrays.asList(emailList2));
        }
        // Send to assigned group manager
        if ( r.group != null && r.group.groupManagers != null) {
            for ( User gm:r.group.groupManagers) {
                if ( !StringUtils.isBlank(gm.email)) {
                    created.add(gm.email.toLowerCase());
                }
            }
        }
        
        // Send creation notification to event admins
        String [] admin2Emails = r.event.adminEmailAddresses.toLowerCase().split(ApplicationHelpers.mailSplit);
        created.addAll(Arrays.asList(admin2Emails));
        
        String [] staticAdmins = ConfigurationHelpers.rfiCreationAdminEmails();
        created.addAll(Arrays.asList(staticAdmins));
        
        // Send creation emails
        for ( String s: created) {
            try {
                notifiers.Mails.rfiCreated(r, s);
            } catch (Exception e) {}
        }
        
        // Send assignement emails
        for ( String s: assigned) {
            try {
                notifiers.Mails.rfiAssigned(r, s);
            } catch (Exception e) {}
        }
        
        if ( ConfigurationHelpers.ogcPublishingEnabled()) {
            try {
                OGCPublishing.insertRFI(r);
            } catch (Exception ex) {
                Logger.error(ex, "Error: OGC publishing - insert");
            }
        }
    }
    
    public static String [] getAllProductFormats() {
        List<String> dbFormats = JPA.em().createQuery("select distinct(productFormat) from RFI").getResultList();
        List<String> defaultProducts = (List<String>)Arrays.asList(ConfigurationHelpers.getProducts());
        Set<String> prods = new TreeSet<String>();
        prods.addAll(dbFormats);
        prods.addAll(defaultProducts);
        
        return prods.toArray(new String[0]);
    }
    
    public static String [] getAllEmailAddresses() {
        List<String> emails = JPA.em().createQuery("select distinct(lower(assignedTo)) from RFI").getResultList();
        Set<String> ret = new HashSet<String>();
        
        for ( String s:emails) {
            if ( s != null) {
                ret.addAll(ApplicationHelpers.extractEmails(s));
            }
        }
        
        String [] retA = ret.toArray(new String[0]);
        Arrays.sort(retA);
        return retA;
    }
    
    public static String [] getAllRequestorEmailAddresses() {
        List<String> emails = JPA.em().createQuery("select distinct(lower(emailAddress)) from RFI").getResultList();
        Set<String> ret = new HashSet<String>();
        
        for ( String s:emails) {
            if ( s != null) {
                ret.addAll(ApplicationHelpers.extractEmails(s));
            }
        }
        
        String [] retA = ret.toArray(new String[0]);
        Arrays.sort(retA);
        return retA;
    }
    
    public static void handleRFIProductAdded(RelatedRFIItem rel) {
        RFI r = RFI.findById(rel.rfi.id);
    
        Activity.LogRFIActivity(r, rel.createdBy, ActivityType.PRODUCT_ADDED);
        
        if ( r.sendEmailToRequestor != null && r.sendEmailToRequestor) {
            Set<String> added = new HashSet<String>();
            Collection<String> reqs = ApplicationHelpers.extractEmails(r.emailAddress);
            
            for (String em:reqs) {
                added.add(em.toLowerCase());
            }
            
            for ( String s:added) {
                try {
                    notifiers.Mails.sendProductAdded(rel, s);
                } catch ( Exception e) {}
            }
        }
    }
    
    public static void handleCommentCreated(Comment c) {
        Activity.LogRFIActivity(c.rfi, c.createdBy, ActivityType.COMMENTED);
        Set<String> commented = new HashSet<String>();
        String myEmail = controllers.Master.getUser().email.toLowerCase();
        
        if ( c.rfi.sendEmailToRequestor != null && c.rfi.sendEmailToRequestor) {
            Collection<String> reqs = ApplicationHelpers.extractEmails(c.rfi.emailAddress);
            
            for (String em:reqs) {
                if ( !em.equals(myEmail)) {
                    commented.add(em);
                }
            }
        }
        
        if ( c.rfi.assignedTo != null) {
            String [] emailList2 = c.rfi.assignedTo.toLowerCase().split(ApplicationHelpers.mailSplit);
            for ( String toEm:emailList2) {
                if ( !toEm.equals(myEmail)) {
                    commented.add(toEm);
                }
            }
        }
        
        for (String s: commented) {
            try {
                notifiers.Mails.commentCreated(c, s);
            } catch (Exception e) {}
        }
    }
    
    public static String [] getAvailableStatuses(RFI r, User u) {
        Logger.debug("%s", u.role);
        ApplicationRole management = ApplicationRole.find("byName", "Management").first();
        ApplicationRole analyst = ApplicationRole.find("byName", "Analyst").first();
        if ( u.role.equals(management)) {
            return new String[] {
                    ApplicationHelpers.PENDING, ApplicationHelpers.ACCEPTED,
                    ApplicationHelpers.IN_WORK, ApplicationHelpers.WITH_CUSTOMER,
                    ApplicationHelpers.COMPLETED,
                    ApplicationHelpers.PERSISTENT, ApplicationHelpers.ON_HOLD,
                    ApplicationHelpers.CANCELED};
        }
        else if ( u.role.equals(analyst)) {
            List<String> ret = new ArrayList<String>();
            if ( r == null) {
                ret.add(ApplicationHelpers.PENDING);
                return ret.toArray(new String[]{});
            }
            ret.add(r.status);
            if ( !StringUtils.isBlank(r.assignedTo)) {
                if ( r.status.equals(ApplicationHelpers.ACCEPTED)) {
                    ret.add(ApplicationHelpers.IN_WORK);
                    ret.add(ApplicationHelpers.WITH_CUSTOMER);
                    ret.add(ApplicationHelpers.PERSISTENT);
                    ret.add(ApplicationHelpers.ON_HOLD);
                }
                else if ( r.status.equals(ApplicationHelpers.IN_WORK)) {
                    ret.add(ApplicationHelpers.WITH_CUSTOMER);
                    ret.add(ApplicationHelpers.PERSISTENT);
                    ret.add(ApplicationHelpers.ON_HOLD);
                }
                else if ( r.status.equals(ApplicationHelpers.ON_HOLD)) {
                    ret.add(ApplicationHelpers.IN_WORK);
                    ret.add(ApplicationHelpers.WITH_CUSTOMER);
                    ret.add(ApplicationHelpers.PERSISTENT);
                }
                else if ( r.status.equals(ApplicationHelpers.PERSISTENT)) {
                    ret.add(ApplicationHelpers.IN_WORK);
                    ret.add(ApplicationHelpers.WITH_CUSTOMER);
                    ret.add(ApplicationHelpers.ON_HOLD);
                }
                else if ( r.status.equals(ApplicationHelpers.WITH_CUSTOMER)) {
                    ret.add(ApplicationHelpers.IN_WORK);
                    ret.add(ApplicationHelpers.ON_HOLD);
                    ret.add(ApplicationHelpers.PERSISTENT);
                }
            }
            
            return ret.toArray(new String[]{});
        }
        else {
            return new String[] { ApplicationHelpers.PENDING};
        }
    }
    
    // Ideally, we'd detach the model from the context vs dealing w/ these one-off changed values posted from the page (yuck)
    public static void handleRFIUpdated(RFI r, String oldAssigned, String oldStatus, String username, String oldGroupId) {
        Logger.debug("OLD: %s New: %s", oldStatus, r.status);
        Logger.debug("OLD GROUP: %s New: %s", oldGroupId, r.group);
        String groupId = "";
        if ( r.group != null) {
            groupId = r.group.id.toString();
        }
        
        if ( r.dateCompleted != null && !oldStatus.equals(ApplicationHelpers.COMPLETED)) {
            r.status = ApplicationHelpers.COMPLETED;
            r.save();
        }
        
        // Log status change
        if ( !oldStatus.equals(ApplicationHelpers.COMPLETED) && r.status.equals(ApplicationHelpers.COMPLETED)) {
            Activity.LogRFIActivity(r, username, ActivityType.COMPLETED);
        }
        else if ( !oldStatus.equals(ApplicationHelpers.IN_WORK) && r.status.equals(ApplicationHelpers.IN_WORK)) {
            Activity.LogRFIActivity(r, username, ActivityType.STARTED);
        }
        else if ( oldStatus.equals(ApplicationHelpers.PENDING) && !r.status.equals(ApplicationHelpers.PENDING)) {
            Activity.LogRFIActivity(r, username, ActivityType.VERIFIED);
        }
        else if ( !oldStatus.equals(ApplicationHelpers.CANCELED) && r.status.equals(ApplicationHelpers.CANCELED)) {
            Activity.LogRFIActivity(r, username, ActivityType.CANCELED);
        }
        else if ( !oldStatus.equals(ApplicationHelpers.PERSISTENT) && r.status.equals(ApplicationHelpers.PERSISTENT)) {
            Activity.LogRFIActivity(r, username, ActivityType.PERSISTENT);
        }
        else if ( !oldStatus.equals(ApplicationHelpers.ON_HOLD) && r.status.equals(ApplicationHelpers.ON_HOLD)) {
            Activity.LogRFIActivity(r, username, ActivityType.ON_HOLD);
        }
        else if ( !oldStatus.equals(ApplicationHelpers.WITH_CUSTOMER) && r.status.equals(ApplicationHelpers.WITH_CUSTOMER)) {
            Activity.LogRFIActivity(r, username, ActivityType.WITH_CUSTOMER);
        }
        else {
            Activity.LogRFIActivity(r, username, ActivityType.CONTENT_UPDATED);
        }
        
        Set<String> groupChanged = new HashSet<String>();
        // Group reassign/assignment - sent to group manager
        if ( !groupId.equals(oldGroupId) && !groupId.equals("")) {
            Group cur = Group.findById(Long.parseLong(groupId));
            if ( cur.groupManagers != null) {
                for ( User gm:cur.groupManagers) {
                    if ( gm.email != null && !gm.email.equals("")) {
                        groupChanged.add(gm.email.toLowerCase());
                    }
                }
            }
        }
        
        for ( String s:groupChanged) {
            try {
                notifiers.Mails.groupChanged(r, s);
            } catch(Exception e) {}
        }
        
        Set<String> statusChange = new HashSet<String>();
        // Send status change to requestor/event admins
        if ( !oldStatus.equals(r.status)) {
            if ( r.sendEmailToRequestor != null && r.sendEmailToRequestor) {
                String [] emailList = r.emailAddress.toLowerCase().split(ApplicationHelpers.mailSplit);
                statusChange.addAll(Arrays.asList(emailList));
            }
            String [] adminEmails = r.event.adminEmailAddresses.toLowerCase().split(ApplicationHelpers.mailSplit);
            statusChange.addAll(Arrays.asList(adminEmails));
        }
        
        for ( String s: statusChange) {
            try {
                notifiers.Mails.sendStatusChange(r, s);
            } catch ( Exception e) {}
        }

        Set<String> assigned = new HashSet<String>();
        // Send specific assignment to event admin, assigned to and requestor
        if ( r.assignedTo != null && !r.assignedTo.trim().equals("") && !r.assignedTo.equals(oldAssigned)) {
            Activity.LogRFIActivity(r, username, ActivityType.ASSIGNED);
            
            if ( r.sendEmailToRequestor != null && r.sendEmailToRequestor) {
                String [] emailList = r.emailAddress.toLowerCase().split(ApplicationHelpers.mailSplit);
                assigned.addAll(Arrays.asList(emailList));
            }

            String [] emailList2 = r.assignedTo.toLowerCase().split(ApplicationHelpers.mailSplit);
            assigned.addAll(Arrays.asList(emailList2));

            String [] emailList3 = r.event.adminEmailAddresses.toLowerCase().split(ApplicationHelpers.mailSplit);
            assigned.addAll(Arrays.asList(emailList3));
        }

        for ( String s:assigned) {
            try {
                notifiers.Mails.rfiAssigned(r, s);
            } catch (Exception e) {}
        }

        if ( ConfigurationHelpers.ogcPublishingEnabled()) {
            try {
                OGCPublishing.updateRFI(r);
            } catch (Exception ex) {
                Logger.error(ex, "Error: OGC publishing - update");
            }
        }
    }

    public static void validateRFI(RFI r, Validation val) {
        val.valid(r);
        // Yes, could implement a custom play.data.validation.Check (@CheckWith)
        for ( String s: r.emailAddress.split(ApplicationHelpers.mailSplit)) {
            val.email("r.emailAddressInvalid", s).message(String.format("Email %s is invalid", s));
        }
        
        if ( r.assignedTo != null && r.assignedTo.trim() != "") {
            for ( String s: r.assignedTo.split(ApplicationHelpers.mailSplit)) {
                val.email("r.assignedToInvalid", s).message(String.format("Email %s is invalid", s));
            }
        }
        
        if (r.dateRequired != null && !r.dateRequired.equals(r.dateRequested)) {
            val.future("r.dateRequired", r.dateRequired, r.dateRequested);
        }
        if (r.nonSpatial == null || r.nonSpatial == false) {
            if ((r.coordinates == null || r.coordinates.lat == 0.0 || r.coordinates.lon == 0.0) && (r.region == null || r.region.equals("")) && (r.polyline == null || r.polyline.equals("")) ) {
                val.addError("r.region", "Please specify a point or a region for the RFI");
            }
        }
        if ( r.organization == null || r.organization.trim().equals(",") || r.organization.trim().equals("")) {
            val.addError("r.organization", "Please specify an organization");
            r.organization = "";
        }
        else {
            Pattern p = Pattern.compile("([^,]+),?$");
            Matcher m = p.matcher(r.organization.trim());
            m.find();
            String org = m.group(1);
            r.organization = org.trim();
        }
        
        if ( r.country == null || r.country.trim().equals("")) {
            val.addError("r.country", "Please select at least one country/region for the RFI");
            r.country = "";
        }
        
        if ( r.productFormat == null || r.productFormat.trim().equals(",") || r.productFormat.trim().equals("")) {
            val.addError("r.productFormat", "Please specify a product format");
            r.productFormat = "";
        }
        else {
            Pattern p = Pattern.compile("([^,]+),?$");
            Matcher m = p.matcher(r.productFormat.trim());
            m.find();
            String prod = m.group(1);
            r.productFormat = prod.trim();
        }
        if ( r.receivedVia != null) {
            Pattern p = Pattern.compile("([^,]+),?$");
            Matcher m = p.matcher(r.receivedVia.trim());
            if ( m.find()) {
                String rv = m.group(1);
                r.receivedVia = rv.trim();
            }
            else {
                r.receivedVia = "";
            }
        }
    }
    
    public static Boolean assignEvent(RFI r) {
        Event try1 = Event.find("byUUID", r.event.uuid).first();
        if ( try1 == null) {
            Event try2 = Event.find("byName", r.event.name).first();
            if ( try2 == null) {
                // oh crap, no event w/ the same name or same uuid...
                Logger.debug(String.format("Couldn't find event for RFI: %s", r.title));
                return false;
            }
            else {
                Logger.debug("Assigning event to RFI by name");
                r.event = try2;
            }
        }
        else {
            r.event = try1;
        }
        return true;
    }
    
    public static Map<String,Object> filter(String search, Long event)
    {
        /*CriteriaBuilder builder = JPA.local.get().em().getCriteriaBuilder();
        CriteriaQuery qry = builder.createQuery(RFI.class);
        Root<RFI> rfiload = qry.from(RFI.class);
        Join<RFI, Comment> comments = rfiload.join("comments", JoinType.LEFT);
        Join<RFI, Event> evt = rfiload.join("event");
        //Join<RFI, Group> group = rfiload.join("group", JoinType.LEFT);
        Predicate notArchived = builder.and(
            builder.or(builder.notEqual(rfiload.get("archived"), 1), builder.isNull(rfiload.get("archived"))),
            builder.or(builder.notEqual(evt.get("archived"), 1), builder.isNull(evt.get("archived"))));
            
        Predicate searchTitle = builder.like(rfiload.<String>get("title"), "%" + search + "%");
        Predicate searchComments = builder.like(comments.<String>get("text"), "%" + search + "%");
        Predicate searchOrg = builder.like(rfiload.<String>get("organization"), "%" + search + "%");
        
        Predicate searchFilter = builder.or(searchTitle, searchComments, searchOrg);
        
        List<Predicate> predicates = new ArrayList<Predicate>();
        predicates.add(searchFilter);
        predicates.add(notArchived);
        
        if ( event != null && !event.equals(0L)) {
            Predicate eventFilter = builder.equal(rfiload.get("event"), Event.findById(evt));
            predicates.add(eventFilter);
        }
        
        qry.where(predicates.toArray(new Predicate[]{}));
        Query query = JPA.local.get().em().createQuery(qry.distinct(true));
        //Logger.debug(query.unwrap(org.hibernate.Query.class).getQueryString());
        CriteriaQuery cquery = qry.select(builder.countDistinct(rfiload));
        Query countQuery = JPA.local.get().em().createQuery(cquery);
        Long totalCount = (Long)countQuery.getSingleResult();
        //Logger.debug(countQuery.unwrap(org.hibernate.Query.class).getQueryString());
        List<RFI> rfis = query.getResultList();
        
        return rfis;*/
        
        return filter(null, null, 0, Integer.MAX_VALUE, true, null, null, event, null, null, search, null, null, null, null, null, null, null, null, null, null, null, null, 0L, null);
    }
    
    static void _assignSort(Root<RFI> rfiload, Join<RFI, Event> event, CriteriaBuilder builder, CriteriaQuery qry, String sort, String sortDirection) {
        if ( sort == null || sort.trim().equals("")) {
            sort = "id";
        }
        if ( sortDirection == null || sortDirection.trim().equals("")) {
            sortDirection = "desc";
        }
        if ( sort.startsWith("event_")) {
            if ( sortDirection.equals("asc")) {
                qry.orderBy(builder.asc(event.get(sort.replace("event_", ""))));
            }
            else {
                qry.orderBy(builder.desc(event.get(sort.replace("event_", ""))));
            }
        }
        else if ( sort.equals("requestor")) {
            if ( sortDirection.equals("asc")) {
                qry.orderBy(builder.asc(builder.concat(rfiload.<String>get("requestorFirstName"), rfiload.<String>get("requestorLastName"))));
            }
            else {
                qry.orderBy(builder.desc(builder.concat(rfiload.<String>get("requestorFirstName"), rfiload.<String>get("requestorLastName"))));
            }
        }
        else {
            if ( sortDirection.equals("asc")) {
                qry.orderBy(builder.asc(rfiload.get(sort)));
            }
            else {
                qry.orderBy(builder.desc(rfiload.get(sort)));
            }
        }
    }
    
    /*
    * @param    me              a non-null reference to the user
    * @param    sort            the field on which to sort the results, defaults to "id"
    *                           (events may be included in the sort by prefixing "event_")
    * @param    sortDirection   either "asc" or "desc", if omitted, this will default to "desc"
    * @return                   either null if the user doesn't have an associated email OR
    *                           a list of RFis
    */
    public static List<RFI> getDirectRFIs(User me, Event evt, String sort, String sortDirection) {
        if ( me.email != null && !me.email.trim().equals("")) {
            List<Predicate> predicates = new ArrayList<Predicate>();
        
            CriteriaBuilder builder = JPA.local.get().em().getCriteriaBuilder();
            CriteriaQuery qry = builder.createQuery(RFI.class);
            Root<RFI> rfiload = qry.from(RFI.class);
            Join<RFI, Event> event = rfiload.join("event");
            
            // NOT archived
            predicates.add(builder.and(
                builder.or(builder.notEqual(rfiload.get("archived"), 1), builder.isNull(rfiload.get("archived"))),
                builder.or(builder.notEqual(event.get("archived"), 1), builder.isNull(event.get("archived")))));
            // Assigned to me OR requested by me
            predicates.add(builder.or(
                builder.like(rfiload.<String>get("emailAddress"), "%" + me.email + "%"),
                builder.like(rfiload.<String>get("assignedTo"), "%" + me.email + "%")));
            if ( evt != null) {
                predicates.add(builder.equal(rfiload.get("event"), evt));
            }
                
            qry.where(predicates.toArray(new Predicate[]{}));
            
            _assignSort(rfiload, event, builder, qry, sort, sortDirection);
            
            Query query = JPA.local.get().em().createQuery(qry.distinct(true));
            Logger.debug(query.unwrap(org.hibernate.Query.class).getQueryString());
            return query.getResultList();
        }
        else {
            return null;
        }
    }
    
    /*
    * Returns a list of RFIs that are in one of the supplied groups AND (optional)
    * associated with the given event, sorted by "sort" in direction "sortDirection"
    * @param    groupIds            list of group IDs that the RFI must be a member of
    * @param    evt                 the event (optional) to filter the RFI list by
    * @param    sort                the field used to sort the RFIs (defaults to "id")
    * @param    sortDirection       the sort direction ("asc" or "desc") the RFIs
    *                               will be listed (defaults to "desc")
    */
    public static List<RFI> getRFIsInGroups(Collection<Long> groupIds, Event evt, String sort, String sortDirection) {
        List<Predicate> predicates = new ArrayList<Predicate>();
        Logger.debug("%s", groupIds.toString());
    
        CriteriaBuilder builder = JPA.local.get().em().getCriteriaBuilder();
        CriteriaQuery qry = builder.createQuery(RFI.class);
        Root<RFI> rfiload = qry.from(RFI.class);
        Join<RFI, Event> event = rfiload.join("event");
        Join<RFI, Group> group = rfiload.join("group", JoinType.LEFT);
        
        // RFI not archived && Event not archived
        predicates.add(builder.and(
            builder.or(builder.notEqual(rfiload.get("archived"), 1), builder.isNull(rfiload.get("archived"))),
            builder.or(builder.notEqual(event.get("archived"), 1), builder.isNull(event.get("archived")))));
        predicates.add(group.get("id").in(groupIds));
        if ( evt != null) {
            predicates.add(builder.equal(rfiload.get("event"), evt));
        }
        qry.where(predicates.toArray(new Predicate[]{}));
        
        _assignSort(rfiload, event, builder, qry, sort, sortDirection);
        Query query = JPA.local.get().em().createQuery(qry.distinct(true));
        return query.getResultList();
    }
    
    //TODO: This function sucks, refactor!!!
    public static Map<String,Object> filter(
        String sort,
        String sortDirection,
        int page,
        int pageSize,
        Boolean active,
        String filterRequestorFirstName,
        String filterRequestorLastName,
        Long filterEvent,
        String filterOrganization,
        String filterAssignee,
        String filterSearch,
        String filterTitle,
        Date filterStartDate,
        Date filterEndDate,
        String filterInstructions,
        String filterCity,
        String filterComment,
        String filterID,
        Long filterGroup,
        Boolean myRFIs,
        String filterCountry,
        String filterBE,
        String filterStatus,
        long filterEventActivity,
        String filterProduct
    ) {
        Map<String,Object> ret = new HashMap<String,Object>();
        
        if ( pageSize <= 0) {
            pageSize = ApplicationHelpers.DEFAULT_PAGE_SIZE;
        }
        
        if ( page < 0) {
            page = 0;
        }
        
        if ( filterEvent == null) {
            filterEvent = 0L;
        }
        if ( filterRequestorFirstName == null) {
            filterRequestorFirstName = "";
        }
        if ( filterRequestorLastName == null) {
            filterRequestorLastName = "";
        }
        if ( filterOrganization == null) {
            filterOrganization = "";
        }
        if ( filterAssignee == null) {
            filterAssignee = "";
        }
        if ( filterSearch == null) {
            filterSearch = "";
        }
        if ( filterTitle == null) {
            filterTitle = "";
        }
        if ( filterInstructions == null) {
            filterInstructions = "";
        }
        if ( filterCity == null) {
            filterCity = "";
        }
        if ( filterComment == null) {
            filterComment = "";
        }
        if ( filterID == null) {
            filterID = "";
        }
        if ( filterGroup == null) {
            filterGroup = 0L;
        }
        if ( filterCountry == null) {
            filterCountry = "";
        }
        if ( filterBE == null) {
            filterBE = "";
        }
        if ( filterStatus == null) {
            filterStatus = "";
        }
        if ( filterProduct == null) {
            filterProduct = "";
        }
        
        Logger.debug("Filters: evt:'%s' req:'%s %s' assignee:'%s' org:'%s' id:'%s' startdate:'%s' enddate:'%s' group:'%s' country:'%s' be:'%s'", filterEvent, filterRequestorFirstName, filterRequestorLastName, filterAssignee, filterOrganization, filterID, filterStartDate, filterEndDate, filterGroup, filterCountry, filterBE);
        Logger.debug("Search: '%s'", filterSearch);
        
        CriteriaBuilder builder = JPA.local.get().em().getCriteriaBuilder();
        CriteriaQuery qry = builder.createQuery(RFI.class);
        //cqry.select(builder.count(cqry.from(RFI.class)));
        Root<RFI> rfiload = qry.from(RFI.class);
        Join<RFI, Comment> comments = rfiload.join("comments", JoinType.LEFT);
        Join<RFI, AdminComment> adminComments = rfiload.join("internalComments", JoinType.LEFT);
        Join<RFI, Event> event = rfiload.join("event");
        //Join<RFI, Group> group = rfiload.join("group", JoinType.LEFT);
        Predicate notArchived = builder.and(
            builder.or(builder.notEqual(rfiload.get("archived"), 1), builder.isNull(rfiload.get("archived"))),
            builder.or(builder.notEqual(event.get("archived"), 1), builder.isNull(event.get("archived"))));
        Predicate archived = builder.or(builder.equal(rfiload.get("archived"), 1), builder.equal(event.get("archived"), 1));
        Predicate requestorFirstNameFilter = builder.like(rfiload.<String>get("requestorFirstName"), "%" + filterRequestorFirstName + "%");
        Predicate requestorLastNameFilter = builder.like(rfiload.<String>get("requestorLastName"), "%" + filterRequestorLastName + "%");
        Predicate assigneeFilter = builder.like(rfiload.<String>get("assignedTo"), "%" + filterAssignee + "%");
        Predicate orgFilter = builder.like(rfiload.<String>get("organization"), "%" + filterOrganization + "%");

        Predicate titleLike = builder.like(rfiload.<String>get("title"), "%" + filterTitle + "%");
        Predicate instructionsLike = builder.like(rfiload.<String>get("instructions"), "%" + filterInstructions + "%");
        Predicate cityNameLike = builder.like(rfiload.<String>get("cityName"), "%" + filterCity + "%");
        Predicate commentTextLike = builder.like(comments.<String>get("text"), "%" + filterComment + "%");
        Predicate countryLike = builder.like(rfiload.<String>get("country"), "%" + filterCountry + "%");
        Predicate beLike = builder.like(rfiload.<String>get("BENumber"), "%" + filterBE + "%");
        Predicate statusEquals = builder.equal(rfiload.get("status"), filterStatus);
        Predicate productEquals = builder.equal(rfiload.<String>get("productFormat"), filterProduct);
        //TODO: Figure out WTF to do since .as(String.class) is killing this...
        //Predicate idLike = builder.like(rfiload.get("id").as(String.class), "%" + filterID + "%");
        
        Predicate searchTitle = builder.like(rfiload.<String>get("title"), "%" + filterSearch + "%");
        Predicate searchComments = builder.like(comments.<String>get("text"), "%" + filterSearch + "%");
        Predicate searchOrg = builder.like(rfiload.<String>get("organization"), "%" + filterSearch + "%");
        Predicate searchCountry = builder.like(rfiload.<String>get("country"), "%<" + filterSearch + ">%");
        Predicate searchBE = builder.like(rfiload.<String>get("BENumber"), "%" + filterSearch + "%");
        
        List<Predicate> searchPredicates = new ArrayList<Predicate>();
        searchPredicates.add(searchTitle);
        searchPredicates.add(searchComments);
        searchPredicates.add(searchOrg);
        searchPredicates.add(searchCountry);
        searchPredicates.add(searchBE);
        
        List<Predicate> predicates = new ArrayList<Predicate>();
        
        if ( !StringUtils.isWhitespace(filterSearch)) {
            Predicate searchIDs = builder.like(rfiload.<String>get("networkID"), "%" + filterSearch + "%");
            searchPredicates.add(searchIDs);
        }
        
        if ( !StringUtils.isWhitespace(filterID)) {
            Predicate searchIDs = builder.like(rfiload.<String>get("networkID"), "%" + filterID + "%");
            predicates.add(searchIDs);
        }
        
        if ( filterStartDate != null) {
            Predicate startDateRequested = builder.greaterThanOrEqualTo(rfiload.<Date>get("dateRequested"), filterStartDate);
            Predicate startDateRequired = builder.greaterThanOrEqualTo(rfiload.<Date>get("dateRequired"), filterStartDate);
            Predicate startDateCompleted = builder.greaterThanOrEqualTo(rfiload.<Date>get("dateCompleted"), filterStartDate);
            predicates.add(builder.or(startDateRequested, startDateRequired, startDateCompleted));
        }
        if ( filterEndDate != null) {
            Predicate endDateRequested = builder.lessThanOrEqualTo(rfiload.<Date>get("dateRequested"), filterEndDate);
            Predicate endDateRequired = builder.lessThanOrEqualTo(rfiload.<Date>get("dateRequired"), filterEndDate);
            Predicate endDateCompleted = builder.lessThanOrEqualTo(rfiload.<Date>get("dateCompleted"), filterEndDate);
            predicates.add(builder.or(endDateRequested, endDateRequired, endDateCompleted));
        }
        
        //TODO: Figure out id in like statement .as(String.class) not working...
        Predicate searchFilter = builder.or(searchPredicates.toArray(new Predicate[]{}));
        
        if ( !filterGroup.equals(0L)) {
            Predicate groupFilter = builder.equal(rfiload.get("group"), Group.findById(filterGroup));
            predicates.add(groupFilter);
        }
        
        if ( !filterEvent.equals(0L)) {
            Predicate eventFilter = builder.equal(rfiload.get("event"), Event.findById(filterEvent));
            predicates.add(eventFilter);
        }
        if ( !filterRequestorFirstName.equals("")) {
            predicates.add(requestorFirstNameFilter);
        }
        if ( !filterRequestorLastName.equals("")) {
            predicates.add(requestorLastNameFilter);
        }
        if ( !filterOrganization.equals("")) {
            predicates.add(orgFilter);
        }
        if ( !filterAssignee.equals("")) {
            predicates.add(assigneeFilter);
        }
        if ( !filterSearch.equals("")) {
            predicates.add(searchFilter);
        }
        if ( !filterTitle.equals("")) {
            predicates.add(titleLike);
        }
        if ( !filterInstructions.equals("")) {
            predicates.add(instructionsLike);
        }
        if ( !filterComment.equals("")) {
            predicates.add(commentTextLike);
        }
        if ( !filterCity.equals("")) {
            predicates.add(cityNameLike);
        }
        if ( !filterCountry.equals("")) {
            predicates.add(countryLike);
        }
        if ( !filterBE.equals("")) {
            predicates.add(beLike);
        }
        if ( !filterStatus.equals("")) {
            predicates.add(statusEquals);
        }
        if ( !filterProduct.equals("")) {
            predicates.add(productEquals);
        }
        if ( filterEventActivity != 0L) {
            Predicate eventActivityEquals = builder.equal(rfiload.get("eventActivity"), EventActivity.findById(filterEventActivity));
            predicates.add(eventActivityEquals);
        }
        
        if ( active != null && active == false) {
            predicates.add(archived);
        }
        else if ( active != null && active == true) {
            predicates.add(notArchived);
        }
        
        if ( BooleanUtils.isTrue(myRFIs)) {
            User me = controllers.Master.getUser();
            List<Predicate> myPredicates = new ArrayList<Predicate>();
            if ( me.email != null && !me.email.equals("")) {
                myPredicates.add(builder.like(rfiload.<String>get("assignedTo"), "%" + me.email + "%"));
                myPredicates.add(builder.like(rfiload.<String>get("emailAddress"), "%" + me.email + "%"));
            }
            if ( me.groups != null && me.groups.size() > 0) {
                myPredicates.add(rfiload.get("group.id").in(Group.collectIds(me.groups)));
            }
            myPredicates.add(notArchived);
            
            Predicate allMyStuff = builder.or(myPredicates.toArray(new Predicate[]{}));
            predicates.add(allMyStuff);
        }
        
        qry.where(predicates.toArray(new Predicate[]{}));
        
        _assignSort(rfiload, event, builder, qry, sort, sortDirection);
        
        Query query = JPA.local.get().em().createQuery(qry.distinct(true));
        Logger.debug(query.unwrap(org.hibernate.Query.class).getQueryString());
        CriteriaQuery cquery = qry.select(builder.countDistinct(rfiload));
        Query countQuery = JPA.local.get().em().createQuery(cquery);
        Long totalCount = (Long)countQuery.getSingleResult();
        //Logger.debug(countQuery.unwrap(org.hibernate.Query.class).getQueryString());
        int totalPages = (int)Math.ceil(totalCount / (float)pageSize);
        
        if ( page >= totalPages && totalPages > 0) {
            page = totalPages - 1;
        }
        else if ( totalPages == 0) {
            page = 0;
        }
        List<RFI> rfis = query.setFirstResult(page*pageSize).setMaxResults(pageSize).getResultList();
        
        ret.put("rfis", rfis);
        ret.put("page", page);
        ret.put("pageSize", pageSize);
        ret.put("totalPages", totalPages);
        ret.put("totalCount", totalCount);
        ret.put("sort", sort);
        ret.put("sortDirection", sortDirection);
        
        return ret;
    }
    
    static void OrderXLSColumns(List<Method> rfiMethods, Class rfiClass) throws NoSuchMethodException {
        Method m = rfiClass.getMethod("getNetworkID");
        int idx = rfiMethods.indexOf(m);
        Collections.swap(rfiMethods, 0, idx);
        
        m = rfiClass.getMethod("getTitle");
        idx = rfiMethods.indexOf(m);
        Collections.swap(rfiMethods, 1, idx);
    }
    
    //TODO: Clean up...magic strings, etc, etc
    public static ByteArrayInputStream exportToXLS(ByteArrayOutputStream str, List<RFI> rfis)
            throws IOException, ClassNotFoundException, IllegalAccessException,
                InvocationTargetException, ParseException, NoSuchMethodException {
        int cellCount = new Integer(0);
        int rowCount = new Integer(0);
        
        String [] toIgnore = { "getAttachments", "getActivities", "getAll", "getOldest", "getByIDs", "getNotArchived"};
        
        Workbook wb = new HSSFWorkbook();
        CreationHelper createHelper = wb.getCreationHelper();
        Sheet s = wb.createSheet("RFIs");

        //Create a style for formatting dates in the excel document
        CellStyle cellDateStyle = wb.createCellStyle();
        cellDateStyle.setDataFormat(createHelper.createDataFormat().getFormat("mm/dd/yyyy h:mm:ss"));
        CellStyle shortDateStyle = wb.createCellStyle();
        shortDateStyle.setDataFormat(createHelper.createDataFormat().getFormat("mm/dd/yyyy"));

        Class rfiClass = Class.forName("models.RFI");
        List<Method> rfiMethods = new ArrayList<Method>(Arrays.asList(rfiClass.getDeclaredMethods()));
        OrderXLSColumns(rfiMethods, rfiClass);

        //Add first row of labels
        Row labelRow = s.createRow((short)rowCount);

        //fill out labels row
        for (Method rfiMethod : rfiMethods) {
            String rfiMethodName = rfiMethod.getName().toString();

            if (rfiMethodName.startsWith("get")
                && !TypeHelpers.inList(toIgnore, rfiMethodName)) {
                
                //add a label for the current method
                Cell cell = labelRow.createCell(cellCount);
                
                if (rfiMethodName.equals("getCoordinates")){
                    cell.setCellValue(rfiMethodName.substring(3) + "(lat,lon)");
                }
                else {
                    cell.setCellValue(rfiMethodName.substring(3));
                }
                cellCount++;
            }
        }
        
        rowCount++;
        
        if (rfis.size() > 0){
            
            for (Object rfi : rfis) {
                Row row = s.createRow((short)rowCount);
                
                cellCount = 0;
                
                for (Method rfiMethod : rfiMethods) {
                    String rfiMethodName = rfiMethod.getName().toString();
                    Cell cell = row.createCell(cellCount);
                    
                    if (rfiMethodName.startsWith("get") && !TypeHelpers.inList(toIgnore, rfiMethodName)) {
                        if (rfiMethod.invoke(rfi) == null){ //getter returns null                            
                            cell.setCellValue("");
                        }
                        else {//getter returns something other than null
                            if (rfiMethodName.contains("Date")) {
                                Date tempDate = (Date)rfiMethod.invoke(rfi);
                                cell.setCellValue(tempDate);
                                cell.setCellStyle(shortDateStyle);
                            }
                            else if (rfiMethodName.equals("getCreatedAt")) {
                                Date tempDate = (Date)rfiMethod.invoke(rfi);
                                cell.setCellValue(tempDate);
                                cell.setCellStyle(cellDateStyle);
                            }
                            else if (rfiMethodName.equals("getCoordinates")){
                                Object coordinate = rfiMethod.invoke(rfi);
                                
                                Class coordinateClass = Class.forName("models.Point");
                                Method coordinateMethods[] = coordinateClass.getDeclaredMethods();
                                String coordinateString = "";
                                
                                for (Method coordinateMethod : coordinateMethods){
                                    String coordinateMethodName = coordinateMethod.getName().toString();
                                    
                                    //getting the lat and lon of the coordinate
                                    if (coordinateMethodName.equals("getLat")){
                                        coordinateString = coordinateMethod.invoke(coordinate).toString() + ", ";
                                    }
                                    if (coordinateMethodName.equals("getLon")){
                                        coordinateString = coordinateString + coordinateMethod.invoke(coordinate).toString();
                                    }
                                }
                                if ( !coordinateString.equals("0.0, 0.0")) {
                                    cell.setCellValue(coordinateString);
                                }
                                else {
                                    cell.setCellValue("");
                                }
                            }
                            else if (rfiMethodName.equals("getEvent")){
                                Object event = rfiMethod.invoke(rfi);
                                
                                Class eventClass = Class.forName("models.Event");
                                Method eventMethods[] = eventClass.getDeclaredMethods();
                                
                                for (Method eventMethod : eventMethods){
                                    String eventMethodName = eventMethod.getName().toString();
                                    
                                    if (eventMethodName.equals("getName")) {
                                        cell.setCellValue(eventMethod.invoke(event).toString());
                                    }
                                }
                            }
                            else if (rfiMethodName.equals("getCountry")) {
                                cell.setCellValue(rfiMethod.invoke(rfi).toString().replaceAll("<", "").replaceAll(">", ""));
                            }
                            else if (rfiMethodName.equals("getRelatedItems")) {
                                List<RelatedRFIItem> items = (List<RelatedRFIItem>)rfiMethod.invoke(rfi);
                                StringBuilder b = new StringBuilder();
                                Class ic = Class.forName("models.RelatedRFIItem");
                                String newline = "";
                                for ( RelatedRFIItem i: items) {
                                    b.append(newline);
                                    b.append(i.toString());
                                    newline = "\n============================================\n";
                                }
                            }
                            else if (rfiMethodName.equals("getInternalComments")) {
                                List<AdminComment> comments = (List<AdminComment>)rfiMethod.invoke(rfi);
                                StringBuilder b = new StringBuilder();
                                Class ac = Class.forName("models.AdminComment");
                                String newline = "";
                                for ( AdminComment c:comments) {
                                    b.append(newline);
                                    b.append(c.toString());
                                    newline = "\n============================================\n";
                                }
                                cell.setCellValue(b.toString());
                            }
                            else if (rfiMethodName.equals("getComments")) {
                                List<Comment> comments = (List<Comment>)rfiMethod.invoke(rfi);
                                StringBuilder b = new StringBuilder();
                                Class cc = Class.forName("models.Comment");
                                String newline = "";
                                for ( Comment c: comments) {
                                    b.append(newline);
                                    b.append(c.toString());
                                    newline = "\n============================================\n";
                                }
                                cell.setCellValue(b.toString());
                            }
                            else {
                                cell.setCellValue(rfiMethod.invoke(rfi).toString());
                            }
                        }
                        
                        cellCount++;
                    }
                }
                rowCount++;
            }
        }
        
        wb.write(str);
        
        ByteArrayInputStream inputStream = new ByteArrayInputStream(str.toByteArray());
        str.close();
        
        return inputStream;
    }
    
    public static String classificationJoinSpaces(String s) {
        return StringUtils.join(s.trim().toUpperCase().replaceAll(",$", "").split(", "), " ");
    }
    
    public static String classificationJoinSlashes(String s) {
        return StringUtils.join(s.trim().toUpperCase().replaceAll(",$", "").split(", "), "/");
    }
    
    public static String [] gatherClassificationEmailStrings(RFI r) {
        if ( StringUtils.isBlank(r.classification)) {
            return new String [] { "Classification: " + ConfigurationHelpers.DefaultClassificationString(),
                "============================================================"};
        }
        StringBuilder b = new StringBuilder();
        b.append("Classification: ");
        b.append(String.format("%s//", r.classification.toUpperCase().replaceAll(",\\s*$", "")));
        
        return new String[] {
            b.toString().replaceAll("/+$", ""),
            "============================================================"};
    }
    
    //TODO: Replace w/ commons
    public static List<RFI> filterOverdue(List<RFI> rfis) {
        List<RFI> ret = new ArrayList<RFI>();
        for ( RFI r: rfis) {
            if ( r.isOverdue()) {
                ret.add(r);
            }
        }
        return ret;
    }
}
