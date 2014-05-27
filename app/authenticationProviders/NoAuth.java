package authenticationProviders;

import play.mvc.*;
import play.*;
import models.User;
import org.apache.commons.lang.*;

/*
* USE ONLY FOR DEBUGGING/DEVELOPING LOCALLY!
*/
public class NoAuth implements IAuthenticationProvider {
    public NoAuth() {
        Logger.warn("WARNING: Security NOT enabled");
    }

    public boolean isAuthed(Http.Request request, Scope.Session session) {
        if ( !session.contains("user") || request.action.equals("Master.setUser")) {
            String username = "admin";
            if ( request.params._contains("user")) {
                username = request.params.get("user");
            }
            return auth(request, session, username, null);
        }
        else {
            return true;
        }
    }
    
    public boolean auth(Http.Request request, Scope.Session session, String username, String password) {
        session.put("user", username);
        return true;
    }
    
    public String userName(Http.Request request, Scope.Session session) {
        return session.get("user").toString();
    }
    
    public void createUserIfDoesntExist(Http.Request request, Scope.Session session) {
        throw new NotImplementedException();
    }
}
