package models;

import helpers.*;

import models.deadbolt.Role;
import models.deadbolt.RoleHolder;
import play.data.validation.Required;
import play.data.binding.NoBinding;
import play.db.jpa.*;
import play.libs.*;
import play.*;

import java.util.*;

import javax.persistence.*;
import javax.persistence.criteria.*;

// User store, captured basic information, User MAY be associated w/ a Group and
// should have exactly 1 role
@Entity(name="Users")
@Table(uniqueConstraints=@UniqueConstraint(columnNames="userName"))
public class User extends Model implements RoleHolder
{
    @Required
    @Column(unique=true, nullable=false)
    public String userName;
    
    @Required
    public String email;

    @Required
    public String firstName;
    
    @Required
    public String lastName;
    
    @Required
    public String agency;
    
    @Required
    public String openPhone;
    
    public String password;
    
    public String resetKey;
    
    public Boolean activated;
    
    @ManyToMany
    @NoBinding("profile")
    public Set<Group> groups;
    
    @ManyToOne
    @NoBinding("profile")
    public ApplicationRole role;

    public User() {}

    public User(String userName,
                String firstName,
                String lastName,
                ApplicationRole role)
    {
        this.userName = userName;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
    }
    
    public Boolean isAdmin() {
        return role.name.equals("Management") || role.name.equals("Analyst");
    }
    
    public static User getByUserName(String userName)
    {
        return find("byUserName", userName).first();
    }
    
    public static Map<String,Object> grid(String sort, String sortDirection, int page, int pageSize, String search) {
        Map<String,Object> ret = new HashMap<String,Object>();
        if ( pageSize <= 0) {
            pageSize = ApplicationHelpers.DEFAULT_PAGE_SIZE;
        }
        if ( page < 0) {
            page = 0;
        }
        if ( sort == null) {
            sort = "userName";
        }
        if ( sortDirection == null) {
            sortDirection = "asc";
        }
        
        Logger.debug("SORT: %s", sort);
        
        CriteriaBuilder builder = JPA.local.get().em().getCriteriaBuilder();
        CriteriaQuery qry = builder.createQuery(User.class);
        Root<User> userLoad = qry.from(User.class);
        
        if ( search != null && !search.equals("")) {
            Predicate p = builder.or(builder.like(userLoad.<String>get("userName"), "%" + search + "%"), builder.like(userLoad.<String>get("email"), "%" + search + "%"));
            qry.where(p);
        }
        
        if ( sortDirection.equals("asc")) {
            qry.orderBy(builder.asc(userLoad.get(sort)));
        }
        else {
            qry.orderBy(builder.desc(userLoad.get(sort)));
        }
        
        Query q = JPA.local.get().em().createQuery(qry);
        List<User> users = q.setFirstResult(page*pageSize).setMaxResults(pageSize).getResultList();
        Long totalCount = (Long)JPA.local.get().em().createQuery(qry.select(builder.countDistinct(userLoad))).getSingleResult();
        
        int totalPages = (int)Math.ceil(totalCount / (float)pageSize);
        
        ret.put("sort", sort);
        ret.put("sortDirection", sortDirection);
        ret.put("page", page);
        ret.put("pageSize", pageSize);
        ret.put("users", users);
        ret.put("totalCount", totalCount);
        ret.put("totalPages", totalPages);
        
        return ret;
    }
    
    @Override
    public String toString()
    {
        return this.userName;
    }

    public List<? extends Role> getRoles()
    {
        return Arrays.asList(role);
    }
}
