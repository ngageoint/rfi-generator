package models;

import controllers.*;
import java.util.*;
import javax.persistence.*;
import helpers.*;

import play.modules.search.*;

import play.db.jpa.*;
import play.*;
import play.data.validation.*;
import play.data.binding.*;

import org.joda.time.*;

import org.hibernate.annotations.Type;

import org.apache.commons.collections.*;

import javax.persistence.*;
import javax.persistence.criteria.*;

/*
Added to support the NST's request:
The NST will have a single event w/ various associated "EventActivities" that an
RFI can be associated w/.  This will ease the NST's use of the RFI generator as
RFI's will be further filterable via this "Event Activity"
*/
@Entity
@Table(name="EventActivities")
public class EventActivity extends Model {
    @Required
    public String name;
    
    @ManyToMany(mappedBy="eventActivities")
    public Set<Event> events;
    
    public String uuid;
    
    @Column(name="created_at", nullable=false)
    @As("yyyy-MM-dd")
    public Date createdAt;
    
    public String createdBy;
    
    @PrePersist
    public void onCreate() {
        if ( this.createdAt == null) {
            this.createdAt = new Date();
        }
        if ( this.uuid == null) {
            this.uuid = UUID.randomUUID().toString();
        }
    }
    
    public EventActivity() {
        this.events = new HashSet<Event>();
    }
}
