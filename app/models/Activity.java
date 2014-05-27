package models;
 
import java.util.*;
import java.text.*;
import javax.persistence.*;

import play.*; 
import play.db.jpa.*;
import play.data.validation.*;
import play.data.binding.*;

import org.joda.time.*;

import org.hibernate.annotations.Type;


// Used for tracking RFI status changes, etc, mainly for reporting
@Entity
@Table(name="Activities")
public class Activity extends Model {
    @ManyToOne
    public RFI rfi;

    public String event;
    
    @Lob
    public String message;
    
    public String targetUser;
    
    @Required
    public String user;
    
    @Enumerated(EnumType.STRING)
    public ActivityType type;

    @Column(name="created_at", nullable=false)
    @As("yyyy-MM-dd")
    public Date createdAt;
    
    @PrePersist
    public void onCreate() {
        if ( this.createdAt == null) {
            this.createdAt = new Date();
        }
    }
    
    private Activity(RFI r, String user, ActivityType type) {
        this.type = type;
        this.rfi = r;
        this.user = user;
    }
    
    private Activity(String user, ActivityType type) {
        this.type = type;
        this.user = user;
    }

    // Is this activity a change in RFI state?
    public static boolean isStateChange(ActivityType t) {
        return (t.equals(ActivityType.CREATED) || t.equals(ActivityType.STARTED)
            || t.equals(ActivityType.COMPLETED) || t.equals(ActivityType.VERIFIED)
            || t.equals(ActivityType.ASSIGNED) || t.equals(ActivityType.CANCELED)
            || t.equals(ActivityType.PERSISTENT) || t.equals(ActivityType.ON_HOLD));
    }
    
    public static void LogRFIActivity(RFI r, String user, ActivityType type) {
        SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm Z");
        if ( isStateChange(type)) {
            if ( Activity.find("rfi = ? and type = ?", r, type).first() != null) {
                return;
            }
        }
        Activity act = new Activity(r, user, type);
        act.message = String.format("RFI - %s (%s) by %s at %s", r.title, type, user, df.format(new Date()));
        Logger.info("ACTIVITY: %s", act.message);
        act.save();
    }
    
    public static void LogEventActivity(Event e, String user, ActivityType type) {
        SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm Z");
        Activity act = new Activity(user, type);
        act.event = e.uuid;
        act.message = String.format("Event - %s (%s) by %s at %s", e.name, type, user, df.format(new Date()));
        Logger.info("ACTIVITY: %s", act.message);
        act.save();
    }
    
    public static void LogUserActivity(User u, String user, ActivityType type) {
        SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm Z");
        Activity act = new Activity(user, type);
        act.targetUser = u.userName;
        act.message = String.format("User - %s (%s) by %s at %s", u.userName, type, user, df.format(new Date()));
        Logger.info("ACTIVITY: %s", act.message);
        act.save();
    }
    
    public static void LogUserActivity(User u, String user, ActivityType type, String addl) {
        SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm Z");
        Activity act = new Activity(user, type);
        act.targetUser = u.userName;
        act.message = String.format("User - %s (%s) by %s at %s (info: %s)", u.userName, type, user, df.format(new Date()), addl);
        Logger.info("ACTIVITY: %s", act.message);
        act.save();
    }
}
