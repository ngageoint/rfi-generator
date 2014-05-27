package models;

import java.util.*;
import javax.persistence.*;
 
import play.db.jpa.*;
import play.data.validation.*;

// Geographic point container, niciety
@Embeddable
public class Point {
    public double lat;
    public double lon;
}
