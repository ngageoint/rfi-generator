package authenticationProviders;

import play.*;
import play.libs.*;
import play.mvc.*;

// Standard inferface that all authentication providers must inherit
public interface IAuthenticationProvider {
    // Is the current request authenticated?
    boolean isAuthed(Http.Request r, Scope.Session s);
    // Try to auth the user using a request OR using a username/password directly if applicable
    boolean auth(Http.Request r, Scope.Session s, String username, String password);
    // Grab the username for the current request/session
    String userName(Http.Request r, Scope.Session s);
    
    void createUserIfDoesntExist(Http.Request r, Scope.Session s);
}
