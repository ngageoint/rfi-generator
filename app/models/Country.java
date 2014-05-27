package models;
 
import java.util.*;
import javax.persistence.*;
 
import play.db.jpa.*;
import play.data.validation.*;

// Used for static dropdown when selecting the associated country for an RFI
@Entity
@Table(name="Countries")
public class Country extends Model
{
    @Required
    public String name;
    
    @Required
    public String code;
    
    public String sortOrder;

    public Country() {}
    
    public static List<Country> getAll() {
        return Country.find("order by sortOrder").fetch();
    }
}
