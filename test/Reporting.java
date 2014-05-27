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
import org.apache.commons.collections.*;
import java.io.*;
import play.libs.*;

import javax.xml.parsers.*;

import javax.persistence.*;
import javax.persistence.criteria.*;

import flexjson.*;
import flexjson.transformer.*;

public class Reporting extends UnitTest {
    @Test
    public void testPeriodicReportingSQL() {
        Query q = JPA.em().createQuery(
            "select new map(sum(case when r.status = :p then 1 else 0 end) as pending, " +
            "sum(case when r.status = :ac then 1 else 0 end) as accepted, " +
            "sum(case when r.status = :iw then 1 else 0 end) as in_work, " +
            "sum(case when r.status = :c then 1 else 0 end) as completed, " +
            "sum(case when r.status = :oh then 1 else 0 end) as on_hold, " +
            "sum(case when r.status = :canceled then 1 else 0 end) as canceled, " +
            "sum(case when r.status = :persistent then 1 else 0 end) as persistent, " +
            "sum(1) as total, " +
            "sum(case when r.status != :c and r.status != :p and r.status != :canceled and r.dateRequired is not null and r.dateRequired < :now then 1 else 0 end) as overdue, " + 
            "r.event.id as event_id, " +
            "(case when r.group.id is not null then r.group.id else 0 end) as group_id) " + 
            "from RFI r where (r.archived is null or r.archived = false) group by r.event.id, r.group.id");
        q.setParameter("p", ApplicationHelpers.PENDING);
        q.setParameter("ac", ApplicationHelpers.ACCEPTED);
        q.setParameter("iw", ApplicationHelpers.IN_WORK);
        q.setParameter("c", ApplicationHelpers.COMPLETED);
        q.setParameter("oh", ApplicationHelpers.ON_HOLD);
        q.setParameter("canceled", ApplicationHelpers.CANCELED);
        q.setParameter("persistent", ApplicationHelpers.PERSISTENT);
        q.setParameter("now", new Date());
        List<Map> items = (List<Map>)q.getResultList();
        // Should initially be 6 items since events 1 and 2 have an associated group (and there are 4 events to begin with)
        Logger.debug(items.toString());
        Logger.debug("%s", items.get(4));
        assertTrue(items.size() == 6);
        assertTrue(items.get(4).get("on_hold").equals(1L));
    }
    
    @Test
    public void testAverages() {
        // Should be 120h for the current test data (432000/60/60)
        assertTrue( ReportingHelpers.averageCreationToCompletionTime(null, null, null, null, null, false).equals(432000.0));
        assertTrue( ReportingHelpers.averageCreationToCompletionTime(null, null, null, null, null, true).equals(432000.0));
        // Only 1 rfi was tracked and took 8 days to complete (started 2012-03-26, completed 2012-04-03)
        assertTrue( ReportingHelpers.averageCreationToCompletionTime(null, null, null, null, 1L, false).equals(691200.0));
        // Only 1 rfi was tracked, time form assignment to completion took 5 days
        assertTrue( ReportingHelpers.averageAssignmentToCompletionTime(null, null, null, null, null, false).equals(432000.0));
        // Should be the average of 2d + 1d + 36h = 36h
        assertTrue( ReportingHelpers.averageCreatedToValidationTime(null, null, null, null, null, false).equals(129600.0));
    }
    
    @Test
    public void testCounts() {
        List<Map> ccounts = ReportingHelpers.getCreatedCount(null, null, null, null);
        Logger.debug("CREATED: %s", ccounts);
        Map m1 = (Map)CollectionUtils.find(ccounts, new org.apache.commons.collections.Predicate() {
            public boolean evaluate(Object o) {
                if ( ((Map)o).get("event").equals("Pentagon NST Event")) {
                    return true;
                }
                else {
                    return false;
                }
            }
        });
        assertTrue(m1.get("count").equals(1L));
        assertTrue(m1.get("date").equals("2012-01-01"));
        
        List<Map> pcounts = ReportingHelpers.getProductFormatCounts(null, null, null, null, null);
        Logger.debug("PRODUCTS: %s", pcounts);
        Map m2 = (Map)CollectionUtils.find(pcounts, new org.apache.commons.collections.Predicate() {
            public boolean evaluate(Object o) {
                Map me = (Map)o;
                if ( me.get("productFormat").equals("JPG")) {
                    return true;
                }
                else {
                    return false;
                }
            }
        });
        assertTrue(m2.get("count").equals(14L));
        
        List<Map> pcounts2 = ReportingHelpers.getProductFormatCounts(null, null, null, null, 1L);
        Logger.debug("PRODUCTS part deux: %s", pcounts2);
        
        Map m3 = (Map)CollectionUtils.find(pcounts2, new org.apache.commons.collections.Predicate() {
            public boolean evaluate(Object o) {
                if ( ((Map)o).get("productFormat").equals("GEOTIF")) {
                    return true;
                }
                else {
                    return false;
                }
            }
        });
        assertTrue(m3.get("count").equals(1L));
        
        List<Map> rfiCounts = ReportingHelpers.getRFICounts(null, null, null);
        Logger.debug("RFI COUNTS: %s", rfiCounts);
        
        Map m4 = (Map)CollectionUtils.find(rfiCounts, new org.apache.commons.collections.Predicate() {
            public boolean evaluate(Object o) {
                Map me = (Map)o;
                if ( me.get("date").equals("2012-04-01") && me.get("event").equals("Hurricane Hilde - Miami, FL")) {
                    return true;
                }
                else {
                    return false;
                }
            }
        });
        // 2 runs on 2012-04-01 => open = (9+13)/2 = 11.5
        assertTrue(m4.get("open").equals(11.5));
    }
}
