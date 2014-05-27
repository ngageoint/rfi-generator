package models;

import models.deadbolt.Role;
import play.data.validation.Required;
import play.db.jpa.Model;
import javax.persistence.*;

import play.db.jpa.*;

import javax.persistence.Entity;

import org.hibernate.annotations.Type;

import org.joda.time.*;

// Meant to be a semi-transient store for use when importing RFIs
@Entity(name="ImportedFiles")
public class ImportedFile extends Model
{
    @Required
    public String attachmentFilename;
    
    @Required
    public Blob associatedFile;
    
    @Column(name="created_at", nullable=false)
    @Type(type="org.joda.time.contrib.hibernate.PersistentDateTime")
    public DateTime createdAt;
    
    public String createdBy;
    
    @PrePersist
    public void onCreate() {
        this.createdAt = new DateTime();
    }
}
