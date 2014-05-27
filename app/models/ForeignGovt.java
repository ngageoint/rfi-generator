package models;
 
import java.util.*;
import javax.persistence.*;
 
import play.db.jpa.*;
import play.data.validation.*;

// Foreign government static lookups (used when classifying the information)
@Entity
@Table(name="ForeignGovt")
public class ForeignGovt extends Model
{
    @Required
    public String name;
    
    @Required
    public String code;
    
    public String sortOrder;

    public ForeignGovt() {}
    
    public static List<ForeignGovt> getAll() {
        return ForeignGovt.find("order by sortOrder").fetch();
    }
}
