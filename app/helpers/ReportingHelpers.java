package helpers;

import models.*;

import play.*;
import play.db.jpa.*;

import java.util.*;
import javax.persistence.*;
import javax.persistence.criteria.*;

// Reporting-related helper functions, would be nice if some of these were using
// criteria queries vs string concatenation.  Even though there is no ability for
// SQL injection, CriteriaQueries (JPA) feel better.
public class ReportingHelpers {
    static Date shimDate1Day(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.add(Calendar.DATE, 1);
        return c.getTime();
    }

    // Used to generate the graph on the "Dashboard" page in the admin experience
    public static long RFIsCreatedOverPeriod(Date start, Date end, Long event, Long group) {
        CriteriaBuilder builder = JPA.local.get().em().getCriteriaBuilder();
        CriteriaQuery qry = builder.createQuery(RFI.class);
        //cqry.select(builder.count(cqry.from(RFI.class)));
        Root<RFI> rfiload = qry.from(RFI.class);
        List<Predicate> predicates = new ArrayList<Predicate>();
        if ( event != null && event != 0) {
            predicates.add(builder.equal(rfiload.get("event"), Event.findById(event)));
        }
        if ( group != null && group != 0) {
            predicates.add(builder.equal(rfiload.get("group"), Group.findById(group)));
        }
        predicates.add(builder.greaterThanOrEqualTo(rfiload.<Date>get("createdAt"), start));
        predicates.add(builder.lessThanOrEqualTo(rfiload.<Date>get("createdAt"), end));
        
        CriteriaQuery cquery = qry.select(builder.countDistinct(rfiload));
        qry.where(predicates.toArray(new Predicate[]{}));
        
        Query countQuery = JPA.local.get().em().createQuery(cquery);
        Long totalCount = (Long)countQuery.getSingleResult();
        return totalCount;
    }
    
    // Used on the "Reports" page in the admin experience for creating the "RFI Totals" graph
    public static List getTotalsOverTime(Long event, Date startDate, Date endDate, Long group, boolean includeSubgroups) {
        if ( endDate == null) {
            endDate = new Date();
        }
        endDate = shimDate1Day(endDate);
        if ( startDate == null) {
            startDate = RFI.getOldest().createdAt;
        }
        
        Logger.info("%s", startDate);
        
        String baseQ = "select avg(s.totalRFIs) as totalRFIs, avg(s.pendingRFIs+s.acceptedRFIs+s.inWorkRFIs+s.persistentRFIs+s.onHoldRFIs+s.withCustomerRFIs) as open, " +
            "avg(s.completedRFIs+s.canceledRFIs) as closed, " +
            "SUBSTRING(s.createdAt, 1, 10) as date, s.event.id from ReportingSnapshot s where s.createdAt >= :start and s.createdAt <= :end ";
            
        if ( event != null && event != 0) {
            baseQ += "and s.event = :event ";
        }
        if ( group != null && group != 0) {
            baseQ += "and s.group in :group ";
        }
        baseQ += "group by s.event.id, SUBSTRING(s.createdAt, 1, 10) order by s.createdAt asc";
        Query q = JPA.em().createQuery(baseQ);
        q.setParameter("start", startDate);
        q.setParameter("end", endDate);
        if ( event != null && event != 0) {
            q.setParameter("event", Event.findById(event));
        }
        if ( group != null && group != 0) {
            Collection<Group> groups = new ArrayList<Group>();
            if ( includeSubgroups) {
                Group g = Group.findById(group);
                groups = g.allDescendentsAndSelf();
            }
            else {
                Group g = Group.findById(group);
                groups.add(g);
            }
            q.setParameter("group", groups);
        }
        return q.getResultList();
    }
    
    
    public static List<Map> getRFICounts(Long event, Date start, Date end) {
        String baseQ = "select new map(avg(s.totalRFIs) as totalRFIs, avg(s.pendingRFIs+s.acceptedRFIs+s.inWorkRFIs+s.persistentRFIs+s.onHoldRFIs+s.withCustomerRFIs) as open, " +
            "avg(s.completedRFIs+s.canceledRFIs) as closed, " +
            "SUBSTRING(s.createdAt, 1, 10) as date, s.event.name as event) from ReportingSnapshot s where 1 = 1 ";
        if ( event != null) {
            baseQ += "and s.event.id = :event ";
        }
        if ( start != null) {
            baseQ += "and s.createdAt >= :start ";
        }
        if ( end != null) {
            end = shimDate1Day(end);
            baseQ += "and s.createdAt <= :end ";
        }
        baseQ += "group by SUBSTRING(s.createdAt, 1, 10), s.event.name";
        Query q = JPA.em().createQuery(baseQ);
        if ( event != null) {
            q.setParameter("event", event);
        }
        if ( start != null) {
            q.setParameter("start", start);
        }
        if ( end != null) {
            q.setParameter("end", end);
        }
        return (List<Map>)q.getResultList();
    }
    
    // Used on the "Reports" page in the admin experience for creating the "RFIs created by date" graph
    public static List<Map> getCreatedCount(Date start, Date end, Long event, Long group) {
        String qry = "select new map(count(r) as count, SUBSTRING(r.createdAt, 1, 10) as date, r.event.name as event) from RFI r where 1 = 1 ";
        if ( start != null) {
            qry += "and r.createdAt >= :start ";
        }
        if ( end != null) {
            end = shimDate1Day(end);
            qry += "and r.createdAt < :end ";
        }
        if ( event != null && event != 0) {
            qry += "and r.event.id = :event ";
        }
        if ( group != null && group != 0) {
            qry += "and r.group.id in :group ";
        }
        qry += "group by SUBSTRING(r.createdAt, 1, 10), r.event.name ";
        Query q = JPA.em().createQuery(qry);
        if ( start != null) {
            q.setParameter("start", start);
        }
        if ( end != null) {
            q.setParameter("end", end);
        }
        if ( event != null && event != 0) {
            q.setParameter("event", event);
        }
        if ( group != null && group != 0) {
            Group g = Group.findById(group);
            q.setParameter("group", Group.collectIds(g.allDescendentsAndSelf()));
        }
        return q.getResultList();
    }
    
    // Used to aggregate the product formats on the "Reports" page in the admin experience
    public static List<Map> getProductFormatCounts(Date start, Date end, Long event, Boolean archived, Long group) {
        String qry = "select new map(count(r) as count, r.productFormat as productFormat) from RFI r where 1 = 1 ";
        if ( start != null) {
            qry += "and r.createdAt >= :start ";
        }
        if ( end != null) {
            end = shimDate1Day(end);
            qry += "and r.createdAt < :end ";
        }
        if ( event != null) {
            qry += "and r.event.id = :event ";
        }
        if ( archived != null && archived == false) {
            qry += "and (r.archived is null or r.archived = false) ";
        }
        if ( group != null && group != 0) {
            qry += "and r.group.id in :group ";
        }
        qry += "group by r.productFormat";
        Query q = JPA.em().createQuery(qry);
        if ( start != null) {
            q.setParameter("start", start);
        }
        if ( end != null) {
            q.setParameter("end", end);
        }
        if ( event != null) {
            q.setParameter("event", event);
        }
        if ( group != null && group != 0) {
            Group g = Group.findById(group);
            q.setParameter("group", Group.collectIds(g.allDescendentsAndSelf()));
        }
        return q.getResultList();
    }
    
    // In seconds - Used on the admin experience's "Dashboard" page AND also the admin experience's "Reports" page
    public static Double averageCreatedToValidationTime(Date start, Date end, Long event, Boolean archived, Long group, boolean includeSubgroups) {
        /*select avg(time_to_sec(timediff(b.created_at,a.created_at)))/3600 as average
            from Activities a, Activities b where a.rfi_id = b.rfi_id and a.type = 'CREATED' and b.type = 'VERIFIED'*/
        String qry = "select avg(time_to_sec(timediff(b.createdAt,a.createdAt))) as average "+
            "from Activity a, Activity b where a.rfi = b.rfi and a.type = 'CREATED' and b.type = 'VERIFIED' ";
        if ( start != null) {
            qry += "and a.createdAt >= :start ";
        }
        if ( end != null) {
            end = shimDate1Day(end);
            qry += "and a.createdAt < :end ";
        }
        if ( event != null && event != 0) {
            qry += "and a.rfi.event.id = :event ";
        }
        if ( archived != null && archived == false) {
            qry += "and (a.rfi.archived is null or a.rfi.archived = false) ";
        }
        if ( group != null && group != 0) {
            qry += "and a.rfi.group.id in :group ";
        }
        Query q = JPA.em().createQuery(qry);
        if ( start != null) {
            q.setParameter("start", start);
        }
        if ( end != null) {
            q.setParameter("end", end);
        }
        if ( event != null && event != 0) {
            q.setParameter("event", event);
        }
        if ( group != null && group != 0) {
            Collection<Long> ids = new ArrayList<Long>();
            if ( includeSubgroups) {
                Group g = Group.findById(group);
                ids = Group.collectIds(g.allDescendentsAndSelf());
            }
            else {
                ids.add(group);
            }
            q.setParameter("group", ids);
        }
        return (Double)q.getSingleResult();
    }
    
    // In seconds - Used on the admin experience's "Dashboard" page AND also the admin experience's "Reports" page
    public static Double averageApprovalToAssignmentTime(Date start, Date end, Long event, Boolean archived, Long group, boolean includeSubgroups) {
        /*select avg(time_to_sec(timediff(b.created_at,a.created_at)))/3600 as average
            from Activities a, Activities b where a.rfi_id = b.rfi_id and a.type = 'VERIFIED' and b.type = 'ASSIGNED'*/
        String qry = "select avg(time_to_sec(timediff(b.createdAt,a.createdAt))) as average "+
            "from Activity a, Activity b where a.rfi = b.rfi and a.type = 'VERIFIED' and b.type = 'ASSIGNED' ";
        if ( start != null) {
            qry += "and a.createdAt >= :start ";
        }
        if ( end != null) {
            end = shimDate1Day(end);
            qry += "and a.createdAt < :end ";
        }
        if ( event != null && event != 0) {
            qry += "and a.rfi.event.id = :event ";
        }
        if ( archived != null && archived == false) {
            qry += "and (a.rfi.archived is null or a.rfi.archived = false) ";
        }
        if ( group != null && group != 0) {
            qry += "and a.rfi.group.id in :group ";
        }
        Query q = JPA.em().createQuery(qry);
        if ( start != null) {
            q.setParameter("start", start);
        }
        if ( end != null) {
            q.setParameter("end", end);
        }
        if ( event != null && event != 0) {
            q.setParameter("event", event);
        }
        if ( group != null && group != 0) {
            Collection<Long> ids = new ArrayList<Long>();
            if ( includeSubgroups) {
                Group g = Group.findById(group);
                ids = Group.collectIds(g.allDescendentsAndSelf());
            }
            else {
                ids.add(group);
            }
            q.setParameter("group", ids);
        }
        return (Double)q.getSingleResult();
    }
    
    // In seconds - Used on the admin experience's "Dashboard" page AND also the admin experience's "Reports" page
    public static Double averageAssignmentToCompletionTime(Date start, Date end, Long event, Boolean archived, Long group, boolean includeSubgroups) {
        /*select avg(time_to_sec(timediff(b.created_at,a.created_at)))/3600 as average
            from Activities a, Activities b where a.rfi_id = b.rfi_id and a.type = 'ASSIGNED' and (b.type = 'COMPLETED' or b.type = 'CANCELED') */
        String qry = "select avg(time_to_sec(timediff(b.createdAt,a.createdAt))) as average "+
            "from Activity a, Activity b where a.rfi = b.rfi and a.type = 'ASSIGNED' and (b.type = 'COMPLETED' or b.type = 'CANCELED') ";
        if ( start != null) {
            qry += "and a.createdAt >= :start ";
        }
        if ( end != null) {
            end = shimDate1Day(end);
            qry += "and a.createdAt < :end ";
        }
        if ( event != null && event != 0) {
            qry += "and a.rfi.event.id = :event ";
        }
        if ( archived != null && archived == false) {
            qry += "and (a.rfi.archived is null or a.rfi.archived = false) ";
        }
        if ( group != null && group != 0) {
            qry += "and a.rfi.group.id in :group ";
        }
        Query q = JPA.em().createQuery(qry);
        if ( start != null) {
            q.setParameter("start", start);
        }
        if ( end != null) {
            q.setParameter("end", end);
        }
        if ( event != null && event != 0) {
            q.setParameter("event", event);
        }
        if ( group != null && group != 0) {
            Collection<Long> ids = new ArrayList<Long>();
            if ( includeSubgroups) {
                Group g = Group.findById(group);
                ids = Group.collectIds(g.allDescendentsAndSelf());
            }
            else {
                ids.add(group);
            }
            q.setParameter("group", ids);
        }
        return (Double)q.getSingleResult();
    }
    
    // In seconds - Used on the admin experience's "Dashboard" page AND also the admin experience's "Reports" page
    public static Double averageCreationToCompletionTime(Date start, Date end, Long event, Boolean archived, Long group, boolean includeSubgroups) {
        /*select avg(time_to_sec(timediff(b.created_at,a.created_at)))/3600 as average
            from Activities a, Activities b where a.rfi_id = b.rfi_id and a.type = 'CREATED' and (b.type = 'COMPLETED' or b.type = 'CANCELED')*/
        String qry = "select avg(time_to_sec(timediff(b.createdAt,a.createdAt))) as average " +
            "from Activity a, Activity b where a.rfi = b.rfi and a.type = 'CREATED' and (b.type = 'COMPLETED' or b.type = 'CANCELED') ";
        if ( start != null) {
            qry += "and a.createdAt >= :start ";
        }
        if ( end != null) {
            end = shimDate1Day(end);
            qry += "and a.createdAt < :end ";
        }
        if ( event != null && event != 0) {
            qry += "and a.rfi.event.id = :event ";
        }
        if ( archived != null && archived == false) {
            qry += "and (a.rfi.archived is null or a.rfi.archived = false) ";
        }
        if ( group != null && group != 0) {
            qry += "and a.rfi.group.id in :group ";
        }
        Query q = JPA.em().createQuery(qry);
        if ( start != null) {
            q.setParameter("start", start);
        }
        if ( end != null) {
            q.setParameter("end", end);
        }
        if ( event != null && event != 0) {
            q.setParameter("event", event);
        }
        if ( group != null && group != 0) {
            Collection<Long> ids = new ArrayList<Long>();
            if ( includeSubgroups) {
                Group g = Group.findById(group);
                ids = Group.collectIds(g.allDescendentsAndSelf());
            }
            else {
                ids.add(group);
            }
            q.setParameter("group", ids);
        }
        return (Double)q.getSingleResult();
    }
    
    public static List<RFI> reportRFIs(Long event, Date start, Date end, Boolean archived, Long group) {
        String qry = "select r from RFI r where 1 = 1 ";
        if ( event != null && event != 0) {
            qry += "and r.event.id = :event ";
        }
        if ( start != null) {
            qry += "and r.createdAt >= :start ";
        }
        if ( end != null) {
            end = shimDate1Day(end);
            qry += "and r.createdAt < :end ";
        }
        if (archived != null && archived == false) {
            qry += "and (r.archived is null or r.archived = false) ";
        }
        if ( group != null && group != 0) {
            qry += "and r.group.id in :group ";
        }
        Query q = JPA.em().createQuery(qry);
        if ( event != null && event != 0) {
            q.setParameter("event", event);
        }
        if ( start != null) {
            q.setParameter("start", start);
        }
        if ( end != null) {
            q.setParameter("end", end);
        }
        if ( group != null && group != 0) {
            Group g = Group.findById(group);
            Collection<Long> grp = Group.collectIds(g.allDescendentsAndSelf());
            q.setParameter("group", grp);
        }
        return (List<RFI>)q.getResultList();
    }
    
    // Used to render the "RFI Statuses by event" graph on the admin experience "Reports" page
    public static List<Map> getRFIStatusesByEvent(Long event, Boolean archived, Long group) {
        String qry = "select new map(sum(case when r.status = :p then 1 else 0 end) as not_verified, " +
            "sum(case when r.status = :ac then 1 else 0 end) as accepted, " +
            "sum(case when r.status = :iw then 1 else 0 end) as in_progress, " +
            "sum(case when r.status = :c then 1 else 0 end) as completed, " +
            "sum(case when r.status = :oh then 1 else 0 end) as on_hold, " +
            "sum(case when r.status = :wc then 1 else 0 end) as with_customer, " + 
            "sum(case when r.status = :persistent then 1 else 0 end) as persistent, " +
            "sum(case when r.status = :canceled then 1 else 0 end) as canceled, r.event.name as event_name)" +
            "from RFI r where 1 = 1 ";
        if ( event != null && event != 0) {
            qry += "and r.event.id = :event ";
        }
        if ( archived != null && archived == false) {
            qry += "and (r.archived = false or r.archived is null) ";
        }
        if ( group != null && group != 0) {
            qry += "and r.group.id in :group ";
        }
        qry += "group by r.event.name ";
        Query q = JPA.em().createQuery(qry);
        q.setParameter("p", ApplicationHelpers.PENDING);
        q.setParameter("ac", ApplicationHelpers.ACCEPTED);
        q.setParameter("iw", ApplicationHelpers.IN_WORK);
        q.setParameter("c", ApplicationHelpers.COMPLETED);
        q.setParameter("oh", ApplicationHelpers.ON_HOLD);
        q.setParameter("wc", ApplicationHelpers.WITH_CUSTOMER);
        q.setParameter("canceled", ApplicationHelpers.CANCELED);
        q.setParameter("persistent", ApplicationHelpers.PERSISTENT);
        if ( event != null && event != 0) {
            q.setParameter("event", event);
        }
        if ( group != null && group != 0) {
            Group g = Group.findById(group);
            Collection<Long> ids = Group.collectIds(g.allDescendentsAndSelf());
            q.setParameter("group", ids);
        }
        return q.getResultList();
    }
}
