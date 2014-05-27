package models;

import controllers.*;
import java.util.*;
import javax.persistence.*;
import helpers.*;

import play.modules.search.*;

import play.db.jpa.*;
import play.*;
import play.data.validation.*;
import play.data.binding.*;

import org.joda.time.*;

import org.hibernate.annotations.Type;

import org.apache.commons.collections.*;

import javax.persistence.*;
import javax.persistence.criteria.*;

// Another organizational entity, associated w/ RFIs, Event AND Users
@Entity
@Table(name="Groups")
public class Group extends Model {
    @Required
    public String name;
    
    @ManyToOne
    public Group parent;
    
    @OneToMany(mappedBy="parent", cascade=CascadeType.ALL)
    public List<Group> children;
    
    @ManyToMany
    public Set<User> groupManagers;
    
    public Boolean defaultGroup;
    
    public String uuid;
    
    @Column(name="created_at", nullable=false)
    @As("yyyy-MM-dd")
    public Date createdAt;
    
    public String createdBy;
    
    @Override
    public String toString() {
        return String.format("%s (%s)", this.name, this.parent != null ? this.parent.name: "No parent");
    }
    
    public String hierarchy() {
        Group g = this;
        StringBuilder b = new StringBuilder();
        b.append("["+g.name+"]");
        while ( g.parent != null) {
            b.insert(0, g.parent.name + " > ");
            g = g.parent;
        }
        return b.toString();
    }
    
    public String getHierarchy() {
        return hierarchy();
    }
    
    @ManyToMany(mappedBy="groups")
    public Set<User> users;
    
    @PrePersist
    public void onCreate() {
        if ( this.createdAt == null) {
            this.createdAt = new Date();
        }
        if ( this.uuid == null) {
            this.uuid = UUID.randomUUID().toString();
        }
    }
    
    public Group(String name) {
        this.name = name;
    }
    
    // Refactor me into a generic grid function
    public static Map<String,Object> grid(String sort, String sortDirection, int page, int pageSize) {
        Map<String,Object> ret = new HashMap<String,Object>();
        if ( pageSize <= 0) {
            pageSize = ApplicationHelpers.DEFAULT_PAGE_SIZE;
        }
        if ( page < 0) {
            page = 0;
        }
        if ( sort == null) {
            sort = "name";
        }
        if ( sortDirection == null) {
            sortDirection = "asc";
        }
        
        CriteriaBuilder builder = JPA.local.get().em().getCriteriaBuilder();
        CriteriaQuery qry = builder.createQuery(Group.class);
        Root<Group> groupLoad = qry.from(Group.class);
        if ( sortDirection.equals("asc")) {
            qry.orderBy(builder.asc(groupLoad.get(sort)));
        }
        else {
            qry.orderBy(builder.desc(groupLoad.get(sort)));
        }
        
        Query q = JPA.local.get().em().createQuery(qry);
        List<User> groups = q.setFirstResult(page*pageSize).setMaxResults(pageSize).getResultList();
        Long totalCount = Group.count();
        
        int totalPages = (int)Math.ceil(totalCount / (float)pageSize);
        
        ret.put("sort", sort);
        ret.put("sortDirection", sortDirection);
        ret.put("page", page);
        ret.put("pageSize", pageSize);
        ret.put("groups", groups);
        ret.put("totalCount", totalCount);
        ret.put("totalPages", totalPages);
        
        return ret;
    }
    
    public static Collection<Long> collectIds(Collection<Group> g) {
        return CollectionUtils.collect(g, new Transformer() {
            public Object transform(Object o) {
                return ((Group)o).id;
            }
        });
    }
    
    public Collection<Group> allDescendentsAndSelf() {
        Set<Group> ret = new HashSet<Group>();
        _allDescendents(ret, this);
        
        return ret;
    }
    
    public void _allDescendents(Collection<Group> cur, Group g) {
        cur.add(g);
        if ( g.children != null && g.children.size() > 0) {
            for ( Group _:g.children) {
                _allDescendents(cur, _);
            }
        }
    }
}
