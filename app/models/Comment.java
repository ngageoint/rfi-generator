package models;

import helpers.*;
import controllers.*;
import java.util.*;
import javax.persistence.*;
 
import play.db.jpa.*;
import play.data.validation.*;
import play.data.binding.*;

import org.joda.time.*;

import org.hibernate.annotations.Type;

// Stores comments meant to be visibile to all, very similar to AdminComment, but accessible
// to anyone in any role
@Entity
@Table(name="Comments")
public class Comment extends Model {
    @Required
    @Lob
    public String text;
    
    @ManyToOne
    public RFI rfi;
    
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
    
    public Comment(String text) {
        this.text = text;
    }
    
    @Override
    public String toString() {
        return String.format("\"%s\"\nCreated by: %s\nCreated at: %s",
            this.text, this.createdBy, ApplicationHelpers.formatDate(this.createdAt));
    }
}
