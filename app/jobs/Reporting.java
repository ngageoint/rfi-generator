package jobs;

import play.*;
import play.jobs.*;
import play.db.jpa.*;
 
import models.*;
import helpers.*;

import java.text.*;
import java.math.*;
import java.util.*;
import javax.persistence.*;
import javax.persistence.criteria.*;

// Do some RFIs/time reporting, runs 1 time per minute during developmand and 1 time per hour in production
@On("cron.reporting")
public class Reporting extends Job {
    public void doJob() {
        if (ConfigurationHelpers.reportingJobEnabled()) {
            Logger.info("Running reports");
            /*select sum(case when r.status = 'Pending' then 1 else 0 end) as pending,
            sum(case when r.status = 'Accepted' then 1 else 0 end) as accepted,
            sum(case when r.status = 'In Work' then 1 else 0 end) as in_work,
            sum(case when r.status = 'Completed' then 1 else 0 end) as completed,
            sum(case when r.status = 'Canceled' then 1 else 0 end) as canceled,
            sum(case when r.status = 'Persistent' then 1 else 0 end) as persistent,
            sum(case when r.status = 'On Hold' then 1 else 0 end) as on_hold,
            sum(case when r.status = 'With Customer' then 1 else 0 end) as with_customer,
            sum(1) as total, 
            sum(case when r.status not in ('Completed', 'Persistent', 'Canceled') and r.dateRequired is not null and r.dateRequired < curdate() then 1 else 0 end) as overdue,
            r.event_id as event_id,
            (case when r.eventActivity_id is not null then r.eventActivity_id else 0 end) as event_activity_id,
            (case when r.group_id is not null then r.group_id else 0 end) as group_id
            from RFIs r where (r.archived is null or r.archived = false) group by r.event_id, r.group_id, r.eventActivity_id*/
            // Switched over to native queries to get the correct aggregations, JPA can't apparently aggregate on null entities
            Query q = JPA.em().createNativeQuery(
                "select sum(case when r.status = 'Pending' then 1 else 0 end) as pending, " +
                "sum(case when r.status = 'Accepted' then 1 else 0 end) as accepted, " + 
                "sum(case when r.status = 'In Work' then 1 else 0 end) as in_work, " + 
                "sum(case when r.status = 'Completed' then 1 else 0 end) as completed, " +
                "sum(case when r.status = 'Canceled' then 1 else 0 end) as canceled, " +
                "sum(case when r.status = 'Persistent' then 1 else 0 end) as persistent, " +
                "sum(case when r.status = 'On Hold' then 1 else 0 end) as on_hold, " +
                "sum(case when r.status = 'With Customer' then 1 else 0 end) as with_customer, " +
                "sum(1) as total, " + 
                "sum(case when r.status not in ('Completed', 'Persistent', 'Canceled') and r.dateRequired is not null and r.dateRequired < curdate() then 1 else 0 end) as overdue, " +
                "r.event_id as event_id, " +
                "(case when r.eventActivity_id is not null then r.eventActivity_id else 0 end) as event_activity_id, " +
                "(case when r.group_id is not null then r.group_id else 0 end) as group_id " +
                "from RFIs r where (r.archived is null or r.archived = false) group by r.event_id, r.group_id, r.eventActivity_id");
            List<Object[]> items = (List<Object[]>)q.getResultList();
            Long t = 0L,p = 0L,n = 0L,i = 0L,c = 0L, o = 0L, canceled = 0L, persistent = 0L, onHold = 0L, withCustomer = 0L;
            for ( Object[] m:items) { 
                ReportingSnapshot s = new ReportingSnapshot("Total RFIs/Open RFIs/Closed RFIs job");
                s.totalRFIs = Long.valueOf(m[8].toString());
                t+=s.totalRFIs;
                s.pendingRFIs = Long.valueOf(m[0].toString());
                p+=s.pendingRFIs;
                s.acceptedRFIs = Long.valueOf(m[1].toString());
                n+=s.acceptedRFIs;
                s.inWorkRFIs = Long.valueOf(m[2].toString());
                i+=s.inWorkRFIs;
                s.completedRFIs = Long.valueOf(m[3].toString());
                c+=s.completedRFIs;
                s.overdueRFIs = Long.valueOf(m[9].toString());
                o+=s.overdueRFIs;
                s.canceledRFIs = Long.valueOf(m[4].toString());
                canceled+=s.canceledRFIs;
                s.persistentRFIs = Long.valueOf(m[5].toString());
                persistent+=s.persistentRFIs;
                s.onHoldRFIs = Long.valueOf(m[6].toString());
                onHold+=s.onHoldRFIs;
                s.withCustomerRFIs = Long.valueOf(m[7].toString());
                withCustomer+=s.withCustomerRFIs;
                s.event = Event.findById(Long.valueOf(m[10].toString()));
                s.eventActivity = EventActivity.findById(Long.valueOf(m[11].toString()));
                if ( !(Long.valueOf(m[12].toString())).equals(0L)) {
                    s.group = Group.findById(Long.valueOf(m[12].toString()));
                }
                s.save();
            }
            
            Logger.info("Reports done (Total RFIs: %d, Pending RFIs: %d, Accepted RFIs: %d, In Work RFIs: %d, Completed RFIs: %d, Overdue RFIs: %d, Canceled RFIs: %d, Persistent RFIs: %d, On Hold RFIs: %d, With Customer RFIs: %d)",
                t, p, n, i, c, o, canceled, persistent, onHold, withCustomer);
        }
    }
}
