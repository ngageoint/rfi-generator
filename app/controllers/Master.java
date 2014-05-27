package controllers;

import play.*;
import play.libs.*;
import play.db.jpa.*;
import play.mvc.*;
import play.vfs.*;

import authenticationProviders.*;

import helpers.*;

import java.util.*;
import java.util.regex.*;
import java.net.URLEncoder;

import javax.persistence.*;
import javax.persistence.criteria.*;

import controllers.deadbolt.*;

import org.yaml.snakeyaml.Yaml;
import org.apache.commons.lang.*;

import models.*;

// Handles the @Before callbacks for request processing, also has some application-wide helpers
// that are tightly bound to the request/response context
public class Master extends Controller {
    static final String AUTH_URL = Play.configuration.get("authURL").toString();
    static final String VALIDATE_URL = Play.configuration.get("validateURL").toString();
    static final String ATTRIBUTES_URL = Play.configuration.get("attributesURL").toString();
    static final String LOGOUT_URL = Play.configuration.get("logoutURL").toString();
    static final String SSO_COOKIE = Play.configuration.get("ssoCookie").toString();
    static final String SSO_HTTP_HEADER = Play.configuration.get("ssoHTTPHeader").toString();
    
    static final String USER_KEY = "user";
    static final String SSO_KEY = "sso";
    static final String IS_ADMIN_KEY = "isAdmin";
    
    static IAuthenticationProvider auth = null;
    static final String BACK_KEY = "back";
    
    public static void RedirectToLogin() {
        redirect(ConfigurationHelpers.GetSSORedirectURL());
    }

    public static boolean isConnected() {
        return session.contains(USER_KEY);
    }

    // DOES THE AUTHENTICATION FOR THE REQUEST
    @Before(unless={"setToken"})
    static void setConnectedUser() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        if ( auth == null) {
            auth = AuthFactory.instance();
        }
        if ( auth.isAuthed(request, session)) {
            if ( !isConnected()) {
                auth.createUserIfDoesntExist(request, session);
            }
            User me = getUser();
            renderArgs.put(USER_KEY, me);
            renderArgs.put(IS_ADMIN_KEY, me.isAdmin());
            session.put(USER_KEY, me.userName);
            Http.Header referer = request.headers.get("referer");
            if ( referer != null) {
                if ( !ApplicationHelpers.refererInRFISite(referer.value())) {
                    renderArgs.put(BACK_KEY, referer.value());
                }
            }
        }
        else {
            redirect(ConfigurationHelpers.GetSSORedirectURL());
        }
    }
    
    // /setUser?user=[USERNAME] - Can be used to login a different user (ONLY IF USING NoAuth Authentication provider)
    public static void setUser(String user) {
        Application.index(null, null , null);
    }
    
    // Get username in the current request context
    public static String connected() {
        return session.get(USER_KEY);
    }
    
    // Get full user model in current request context
    public static User getUser() {
        return User.find("byUsername", session.get(USER_KEY)).first();
    }
    
    // If you want to do full-circle testing w/ the OpenAM authentication provider, you can
    // hit this w/ your GeoINT username/password to get a valid token
    public static void setToken(String username, String password) {
        if ( ConfigurationHelpers.isSecurityBypassed() &&
                auth.auth(request, session, username, password))
        {
            String cookieVal = session.get(SSO_KEY).replaceAll(String.format("%s=", SSO_COOKIE), "");
            session.remove(USER_KEY);
            response.setCookie(SSO_COOKIE, cookieVal.replace("\n","").replace("\r",""));
            Application.index(null,null,null);
        }
        else {
            error(String.format("Unable to authenticate: %s", username));
        }
    }
}
