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

// Used to store internal-only comments (only viewable to members of the "management" and "analyst" roles)
@Entity
@Table(name="AdminComments")
public class AdminComment extends Model {
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
    
    public AdminComment(String text) {
        this.text = text;
    }
    
    @Override
    public String toString() {
        return String.format("\"%s\"\nCreated by: %s\nCreated at: %s",
            this.text, this.createdBy, ApplicationHelpers.formatDate(this.createdAt));
    }
}
