package models;

import models.deadbolt.Role;
import play.data.validation.Required;
import javax.persistence.*;

import play.db.jpa.*;
import play.data.binding.*;

import java.util.*;

import javax.persistence.Entity;

import org.hibernate.annotations.Type;

import org.joda.time.*;

// Not really used (in current production configurations), but it meant to store
// file attachments
@Entity(name="Attachments")
public class Attachment extends Model
{
    @Required
    @ManyToOne
    public RFI rfi;
    
    @Required
    public String attachmentFilename;
    
    @Required
    @Lob
    @Basic(fetch=FetchType.LAZY)
    public String fileContent;
    
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
}
