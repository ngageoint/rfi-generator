package models;

import helpers.ApplicationHelpers;
 
import java.util.*;
import javax.persistence.*;

import play.db.jpa.*;
import play.data.validation.*;
import play.data.binding.*;

import org.joda.time.*;

import org.hibernate.annotations.Type;

import javax.persistence.*;
import javax.persistence.criteria.*;

/* One of the main business entities:
ALL RFIs are associated w/ an event, an "event" is the programmatic term that maps
to a business-level "Event or disaster the R3 team is supporting"
*/
@Entity
@Table(name="Events")
public class Event extends Model {
    @Required
    public String name;
    
    public Boolean archived;
    
    public String smtsCategory;
    
    @ManyToMany(fetch=FetchType.EAGER)
    public Set<Group> groups;
    
    @Lob
    @Required
    public String region;
    
    @Required
    @Lob
    public String description;
    
    @ManyToMany(cascade=CascadeType.ALL)
    public Set<EventActivity> eventActivities;
    
    @Required
    public String adminEmailAddresses;
    
    public String uuid;
    
    public String geoQId;
    
    @OneToMany(mappedBy="event", cascade=CascadeType.ALL)
    public List<RFI> rfis;

    public Event() {
        this.archived = false;
        this.rfis = new ArrayList<RFI>();
        this.groups = new HashSet<Group>();
        this.eventActivities = new HashSet<EventActivity>();
    }
    
    public String createdBy;
    
    @Column(name="created_at", nullable=false)
    @As("yyyy-MM-dd")
    public Date createdAt;
    
    @PrePersist
    public void onCreate() {
        if ( this.createdAt == null) {
            this.createdAt = new Date();
        }
        if ( this.uuid == null) {
            this.uuid = UUID.randomUUID().toString();
        }
    }
    
    @PostPersist
    public void onAfterCreate() {
        if ( this.networkID == null || this.networkID.equals("")) {
            this.networkID = ApplicationHelpers.getIDString(this.id);
        }
    }
    
    public static Long getCompletedRFICount(Event e) {
        return RFI.count("event = ? and (status = 'COMPLETED' or status = 'CANCELED')", e);
    }
    
    public String networkID;
    
    public static Long getActiveRFICount(Event e) {
        return RFI.count("event = ? and (archived is null or archived = 0)", e);
    }
    
    public static List<Event> getActive() {
        return Event.find("archived is null or archived = 0 order by createdAt desc").fetch();
    }
    
    // Only used in the RFI import
    public static Event fuzzyFind(Event e) {
        Event try1 = Event.find("byUUID", e.uuid).first();
        if ( try1 == null) {
            return Event.find("byName", e.name).first();
        }
        else {
            return try1;
        }
    }
    
    public static Map<String,Object> grid(String sort, String sortDirection, int page, int pageSize, Boolean active) {
        Map<String,Object> ret = new HashMap<String,Object>();
        
        if ( sort == null) {
            sort = "createdAt";
        }
        if ( sortDirection == null) {
            sortDirection = "desc";
        }
        
        if ( page < 0) {
            page = 0;
        }
        
        if ( pageSize <= 0) {
            pageSize = ApplicationHelpers.DEFAULT_PAGE_SIZE;
        }
        
        CriteriaBuilder builder = JPA.local.get().em().getCriteriaBuilder();
        CriteriaQuery qry = builder.createQuery(Event.class);
        CriteriaQuery cqry = builder.createQuery(Long.class);
        cqry.select(builder.count(cqry.from(Event.class)));
        Root<Event> eventLoad = qry.from(Event.class);
        Predicate notArchived = builder.equal(eventLoad.get("archived"), 0);
        Predicate archived = builder.equal(eventLoad.get("archived"), 1);
        Predicate archivedNotNull = builder.isNotNull(eventLoad.get("archived"));
        
        if ( active != null && active == false) {
            cqry.where(builder.and(archivedNotNull, archived));
            qry.where(builder.and(archivedNotNull, archived)); 
        }
        else if ( active != null && active == true) {
            cqry.where(builder.and(archivedNotNull, notArchived));
            qry.where(builder.and(archivedNotNull, notArchived));
        }
        
        if ( sortDirection.equals("asc")) {
            qry.orderBy(builder.asc(eventLoad.get(sort)));
        }
        else {
            qry.orderBy(builder.desc(eventLoad.get(sort)));
        }
       
        Query query = JPA.local.get().em().createQuery(qry);
        List<Event> events = query.setFirstResult(page*pageSize).setMaxResults(pageSize).getResultList();
        Long totalCount = (Long)JPA.local.get().em().createQuery(cqry).getSingleResult();
        
        int totalPages = (int)Math.ceil(totalCount / (float)pageSize);
        
        ret.put("totalPages", totalPages);
        ret.put("totalCount", totalCount);
        ret.put("events", events);
        ret.put("sort", sort);
        ret.put("sortDirection", sortDirection);
        ret.put("page", page);
        ret.put("pageSize", pageSize);
        ret.put("active", active);

        return ret;        
    }
    
    public static List<Event> getAllNotArchived() {
        return Event.find("archived is null or archived = false").fetch();
    }
    
    public static List<Event> getAll() {
        return Event.all().fetch();
    }
}
