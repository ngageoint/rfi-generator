package authenticationProviders;

import play.*;
import play.mvc.*;
import org.apache.commons.lang.*;

import java.util.regex.*;

import helpers.*;
import models.*;

/*
* GeoAxis authentication provider - simply uses HTTP header to determine if the
* user is authed AND their username, no email provided. :(
*/
public class GeoAxis implements IAuthenticationProvider {
    static final String SSO_HTTP_HEADER = Play.configuration.get("ssoHTTPHeader").toString();
    static final Pattern extractor = Pattern.compile(Play.configuration.get("geoAxisRegex").toString(), Pattern.CASE_INSENSITIVE);
    static final String USER_KEY = "user";
    
    public GeoAxis() {
        Logger.info("Using geoaxis auth - http request header: '%s', regex to extract username: '%s'", SSO_HTTP_HEADER, extractor.toString());
    }

    public boolean isAuthed(Http.Request request, Scope.Session session) {
        Logger.debug(request.headers.toString());
        return request.headers.containsKey(SSO_HTTP_HEADER);
    }
    
    public String userName(Http.Request request, Scope.Session session) {
        Matcher m = extractor.matcher(request.headers.get(SSO_HTTP_HEADER).value());
        m.matches();
        Logger.debug("Username: %s", m.group(1));
        return m.group(1);
    }
    
    public boolean auth(Http.Request request, Scope.Session session, String username, String password) {
        throw new NotImplementedException();
    }

    public void createUserIfDoesntExist(Http.Request request, Scope.Session session) {
        String username = userName(request, session);
        
        if ( User.find("byUsername", username).first() == null) {
            User u = new User();
            u.userName = username;
            u.activated = true;
            u.email = "";
            u.openPhone = "";
            u.firstName = "";
            u.lastName = "";
            u.role = ConfigurationHelpers.getMagicRoleForUsername(username);
            u.save();
            Activity.LogUserActivity(u, username, ActivityType.CREATED, u.role.name);
        }
        
        session.put(USER_KEY, username);
    }
}
