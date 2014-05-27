package controllers;

import play.*;
import play.libs.*;
import play.db.jpa.*;
import play.mvc.*;
import play.data.validation.*;
import play.data.binding.*;

import notifiers.*;

import helpers.*;

import java.util.*;

import javax.persistence.*;
import javax.persistence.criteria.*;

import controllers.deadbolt.*;
import java.util.regex.*;

import models.*;

import org.joda.time.*;

import com.google.gson.*;

// User management on the map experience
@With({Master.class, Deadbolt.class})
public class UserManagement extends BaseController {
    @Restrict("Management")
    public static void grid(int page, String sort, String sortDirection) {
        if ( sort == null) {
            sort = "id";
        }
        if ( sortDirection == null) {
            sortDirection = "asc";
        }
        
        int page_size = ApplicationHelpers.DEFAULT_PAGE_SIZE;
        
        String _params = String.format("page=%d&sort=%s&sortDirection=%s", page, sort, sortDirection);
        
        CriteriaBuilder builder = JPA.local.get().em().getCriteriaBuilder();
        CriteriaQuery qry = builder.createQuery(User.class);
        Root<User> userLoad = qry.from(User.class);
        if ( sortDirection.equals("asc")) {
            qry.orderBy(builder.asc(userLoad.get(sort)));
        }
        else {
            qry.orderBy(builder.desc(userLoad.get(sort)));
        }
       
        Query query = JPA.local.get().em().createQuery(qry);
        List<User> users = query.getResultList();
        long totalCount = users.size();
        
        render(users, totalCount, page_size, _params);
    }
    
    @Restrict("Management")
    public static void edit(Long id) {
        User u = User.findById(id);
        if ( u == null) {
            notFound("User not found");
        }
        render(u);
    }
    
    public static void updateMyInfo() {
        User u = Master.getUser();
        render(u);
    }
    
    // the @As("profile") prevents update of the Role/group...w/o this = major security hole
    public static void submitUpdateMyInfo(@As("profile") User u) {
        User me = Master.getUser();
        if ( u.id == me.id) {
            me.email = u.email;
            me.firstName = u.firstName;
            me.lastName = u.lastName;
            me.openPhone = u.openPhone;
            Pattern p = Pattern.compile("(\\w+),?$");
            Matcher m = p.matcher(u.agency.trim());
            if ( m.find()) {
                String org = m.group(1);
                u.agency = org.trim();
            }
            else {
                u.agency = "";
            }
            me.agency = u.agency;
            me.save();
            flash.success("Your information has been updated");
        }
        else {
            flash.error("Error updating your user information");
        }
        u = me;
        renderTemplate("UserManagement/updateMyInfo.html", u);
    }
    
    @Restrict("Management")
    public static void update(@Valid User u, Boolean activated) {
        Pattern p = Pattern.compile("(\\w+),?$");
        Matcher m = p.matcher(u.agency.trim());
        if ( m.find()) {
            String org = m.group(1);
            u.agency = org.trim();
        }
        else {
            u.agency = "";
        }
    
        ValidateUser(u);
        if ( !validation.hasErrors())  {
            u.email = u.email.toLowerCase();
            u.save();
            flash.success("User successfully updated");
            renderTemplate("UserManagement/edit.html", u);
        }
        else {
            flash.clear();
            renderTemplate("UserManagement/edit.html", u);
        }
    }
    
    static void ValidateUser(User u) {
        validation.required("u.agency", u.agency);
        validation.email("u.email", u.email);
    }
}
