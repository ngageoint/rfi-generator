package models;

import controllers.*;
import helpers.*;

import java.util.*;
import javax.persistence.*;
import javax.persistence.criteria.*;

import play.*;
import play.data.binding.*;
import play.mvc.*;

import play.db.jpa.*;
import play.data.validation.*;

import org.hibernate.annotations.Type;

import org.apache.commons.lang.BooleanUtils;

import org.joda.time.*;

// This model is central to the application
// An RFI contains information about a request for information that is submitted to NGA
// for fulfillment (be that creating a product, etc).  An RFI is always associated
// w/ an Event and may be optionally associated w/ an EventActivity if the Event has
// associated activities
@Entity
@Table(name="RFIs")
public class RFI extends Model {

    // Primary info
    @Required
    public String title;
    
    @Lob
    @Required
    public String instructions;
    
    // Ancillary info
    
    @Required
    @As("yyyy-MM-dd")
    public Date dateRequested;
    
    @As("yyyy-MM-dd")
    public Date dateRequired;
    
    @As("yyyy-MM-dd")
    public Date dateCompleted;
    
    @Required
    public String productFormat;
    
    @Required
    public String requestorFirstName;
    
    @Required
    public String requestorLastName;
    
    @Required
    public String emailAddress;
    
    @Required
    public String organization;
    
    public String phoneOpen;
    
    public String phoneSecure;
    
    public String address1;

    public String address2;

    public String cityName;
    
    public String state;
    
    public String country;
    
    public String zipcode;
    
    @Embedded
    public Point coordinates;
    
    @Lob
    public String region;
    
    @Lob
    public String polyline;
    
    public Boolean nonSpatial;
    
    @OneToMany(mappedBy="rfi", cascade=CascadeType.ALL, orphanRemoval=true)
    public List<Attachment> attachments;
    
    public Boolean sendEmailToRequestor;
    
    public String status;
    
    public String assignedTo;
    
    public Boolean archived;
    
    public String BENumber;
    
    public String classification;
    
    public String uuid;
    
    public String receivedVia;
    public String taskNumber;
    public String assignedPhoneOpen;
    public String assignedPhoneSecure;
    public String workHours;
    
    public String geoQJobId;
    
    @OneToMany( mappedBy="rfi", cascade=CascadeType.ALL, orphanRemoval=true)
    public List<RelatedRFIItem> relatedItems;
    
    @OneToMany( mappedBy="rfi", cascade=CascadeType.ALL, orphanRemoval=true)
    public List<AdminComment> internalComments;
    
    @ManyToOne
    public Event event;
    
    @ManyToOne
    public EventActivity eventActivity;
    
    @ManyToOne
    public Group group;
    
    @OneToMany( mappedBy="rfi", cascade=CascadeType.ALL, orphanRemoval=true)
    public List<Activity> activities;
    
    @OneToMany( mappedBy="rfi", cascade=CascadeType.ALL, orphanRemoval=true)
    public List<Comment> comments;
    
    public String smtsKeywords;
    
    public String createdBy;
    
    public String networkID;
    
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
    
    public Boolean isOverdue() {
        Date now = new Date();
        if ( this.dateRequired != null && now.after(this.dateRequired)
            && !this.status.equals(ApplicationHelpers.COMPLETED)
            && !this.status.equals(ApplicationHelpers.CANCELED))
        {
            return true;
        }
        else {
            return false;
        }
    }
    
    public boolean isPending() {
        if ( this.status.equals(ApplicationHelpers.PENDING)) {
            return true;
        }
        else {
            return false;
        }
    }
    
    public Boolean isActive() {
        Date now = new Date();
        if ( this.status.equals(ApplicationHelpers.IN_WORK) || this.status.equals(ApplicationHelpers.ACCEPTED)
            || this.status.equals(ApplicationHelpers.PERSISTENT) || this.status.equals(ApplicationHelpers.ON_HOLD)
            || this.status.equals(ApplicationHelpers.WITH_CUSTOMER))
        {
            return true;
        }
        else {
            return false;
        }
    }
    
    public boolean isArchived() {
        return BooleanUtils.isTrue(this.archived) || BooleanUtils.isTrue(this.event.archived);
    }
    
    public Boolean isClosed() {
        if ( this.status.equals(ApplicationHelpers.COMPLETED) || this.status.equals(ApplicationHelpers.CANCELED))
        {
            return true;
        }
        else {
            return false;
        }
    }
    
    public RFI() {}
    
    public static List<RFI> getNotArchived() {
        return RFI.find("archived is null or archived = 0").fetch();
    }
    
    public static List<RFI> getByIDs(List<Long> ids) {
        return find("id in :ids").bind("ids", ids).fetch();
    }
    
    public Boolean isMine(User u) {
        Boolean ret = false;
        if ( u.email != null && !u.email.equals("")) {
            if ( this.emailAddress != null && !this.emailAddress.equals("")) {
                ret |= this.emailAddress.toLowerCase().contains(u.email.toLowerCase());
            }
            if ( this.assignedTo != null && !this.assignedTo.equals("")) {
                ret |= this.assignedTo.toLowerCase().contains(u.email.toLowerCase());
            }
            return ret;
        }
        else {
            return false;
        }
    }
    
    public static List<RFI> getAll() {
        return RFI.all().fetch();
    }
    
    public static RFI getOldest() {
        return RFI.find("order by createdAt asc").first();
    }
}
