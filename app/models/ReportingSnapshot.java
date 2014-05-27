package models;
 
import controllers.*;
import java.util.*;
import javax.persistence.*;
import javax.persistence.criteria.*;
 
import play.db.jpa.*;
import play.data.validation.*;
import play.data.binding.*;

import org.joda.time.*;

import org.hibernate.annotations.Type;

// Used to capture the hourly (current production configuration) reporting snapshots needed
// to create the dashboard and some of the reporting page graphs
@Entity
@Table(name="ReportingSnapshots")
public class ReportingSnapshot extends Model {
    @Required
    @Lob
    public String text;
    
    @ManyToOne
    public Event event;
    @ManyToOne
    public EventActivity eventActivity;
    public Long totalRFIs;
    public Long pendingRFIs;
    public Long inWorkRFIs;
    public Long acceptedRFIs;
    public Long completedRFIs;
    public Long onHoldRFIs;
    public Long overdueRFIs;
    public Long canceledRFIs;
    public Long persistentRFIs;
    public Long withCustomerRFIs;
    
    @ManyToOne
    public Group group;
    
    @Column(name="created_at", nullable=false)
    @As("yyyy-MM-dd")
    public Date createdAt;
    
    @PrePersist
    public void onCreate() {
        if ( this.createdAt == null) {
            this.createdAt = new Date();
        }
    }
    
    public ReportingSnapshot(String text) {
        this.text = text;
    }
}
