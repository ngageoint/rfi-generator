package models;
 
import controllers.*;
import java.util.*;
import javax.persistence.*;
 
import play.db.jpa.*;
import play.data.validation.*;
import play.data.binding.*;

import org.joda.time.*;

import org.hibernate.annotations.Type;

// Not currently used, was GOING to use this to track database data migrations (for automation)
@Entity
@Table
public class Migration extends Model {
    @Required
    public String version;
    
    public Migration() {
    }
}
