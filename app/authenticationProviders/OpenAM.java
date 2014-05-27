package authenticationProviders;

import play.*;
import play.mvc.*;
import play.libs.*;

import models.*;
import helpers.*;

import org.apache.commons.lang.*;

import java.net.URLEncoder;

/*
* OpenAM/OpenSSO auth provider - uses token-based validation, which *might* not
* even need to be done since this *should* be processed AFTER apache has already validated
* the user
*/
public class OpenAM implements IAuthenticationProvider {
    static final String SSO_COOKIE = Play.configuration.get("ssoCookie").toString();
    static final String VALIDATE_URL = Play.configuration.get("validateURL").toString();
    static final String AUTH_URL = Play.configuration.get("authURL").toString();
    static final String ATTRIBUTES_URL = Play.configuration.get("attributesURL").toString();
    static final String LOGOUT_URL = Play.configuration.get("logoutURL").toString();
    static final String SSO_HTTP_HEADER = Play.configuration.get("ssoHTTPHeader").toString();

    static final String SSO_KEY = "sso";
    static final String USER_KEY = "user";

    // Do necessary token validation in addition to request validation
    public boolean isAuthed(Http.Request request, Scope.Session session) {
        if ( request.cookies.containsKey(SSO_COOKIE)) {
            return validateToken(request, session);
        }
        else {
            return false;
        }
    }
    
    boolean validateToken(Http.Request request, Scope.Session session) {
        String token = String.format("%s=%s", SSO_COOKIE, request.cookies.get(SSO_COOKIE).value);
        
        WS.WSRequest req = WS.url(VALIDATE_URL);
        req.setHeader("Cookie", token.replace("\n","").replace("\r",""));
        WS.HttpResponse resp = req.get();
        String body = resp.getString().replace("\n","").replace("\r","");
        if ( resp.success() && body.equals("boolean=true")) {
            session.put(SSO_KEY, token);
            return true;
        }
        else {
            return false;
        }
    }
    
    public String userName(Http.Request request, Scope.Session session) {
        return session.get(USER_KEY);
    }
    
    public boolean auth(Http.Request request, Scope.Session session, String username, String password) {
        Logger.info("PARAMS: %s", request.params.toString());
        String usr = String.format("&username=%s&password=%s", URLEncoder.encode(username), URLEncoder.encode(password));
        String reqUrl = AUTH_URL + usr;
        WS.WSRequest req = WS.url(reqUrl);
        WS.HttpResponse resp = req.get();
        if ( resp.success()) {
            String body = resp.getString();
            session.put(SSO_KEY, body.replace("token.id", SSO_COOKIE));
            return true;
        }
        else {
            session.remove(SSO_KEY);
            Logger.warn("Error authenticating: %s", username);
            return false;
        }
    }
    
    String getAttributes(String token, Http.Request request, Scope.Session session) {
        String rawToken = token.replace(String.format("%s=",SSO_COOKIE), "");
        String attrUrl = String.format("%s&subjectid=%s", ATTRIBUTES_URL, rawToken);
        WS.WSRequest req = WS.url(attrUrl);
        req.setHeader("Cookie", token.replace("\n","").replace("\r",""));
        WS.HttpResponse resp = req.get();
        if ( resp.success()) {
            String attrs = resp.getString();
            Logger.info("ATTRIBUTES: %s", attrs);
            return attrs;
        }
        else {
            Http.Header ssoUser = request.headers.get(SSO_HTTP_HEADER);
            if ( ssoUser != null && !ssoUser.value().trim().equals("")) {
                return String.format("userdetails.attribute.name=uid\nuserdetails.attribute.value=%s", ssoUser.value());
            }
            else {
                Logger.warn("Error fetching attrs (probably a CAC user), using \"userdetails.attribute.name=uid\nuserdetails.attribute.value=cacuser\"");
                return "userdetails.attribute.name=uid\nuserdetails.attribute.value=cacuser";
            }
        }
    }

    public void createUserIfDoesntExist(Http.Request request, Scope.Session session) {
        String allAttrs = getAttributes(session.get(SSO_KEY), request, session);
        String [] attrs = allAttrs.split("\\n");
        // Barf, find corresponding attributes line-by-line
        String valReg = "^userdetails\\.attribute\\.value=";
        String email = "";
        String tel = "";
        String sn = "";
        String givenName = "";
        String name = "";
        String username = "";
        for ( int i = 0; i < attrs.length; i++) {
            if ( attrs[i].matches("^userdetails\\.attribute\\.name=mail")) {
                email = attrs[i+1].replaceAll(valReg, "");
            }
            if ( attrs[i].matches("^userdetails\\.attribute\\.name=telephonenumber")) {
                tel = attrs[i+1].replaceAll(valReg, "");
            }
            if ( attrs[i].matches("^userdetails\\.attribute\\.name=sn")) {
                sn = attrs[i+1].replaceAll(valReg, "");
            }
            if ( attrs[i].matches("^userdetails\\.attribute\\.name=name")) {
                name = attrs[i+1].replaceAll(valReg, "");
            }
            if ( attrs[i].matches("^userdetails\\.attribute\\.name=givenname")) {
                givenName = attrs[i+1].replaceAll(valReg, "");
            }
            if ( attrs[i].matches("^userdetails\\.attribute\\.name=uid")) {
                username = attrs[i+1].replaceAll(valReg, "");
            }
        }
        
        username = username.equals("") ? sn: username;
        
        if ( User.find("byUsername", username).first() == null) {
            User u = new User();
            u.userName = username;
            u.activated = true;
            u.email = email;
            u.openPhone = tel;
            if ( name.equals("")) {
                u.firstName = givenName;
                u.lastName = sn;
            }
            else {
                u.firstName = name;
                u.lastName = name;
                //TODO: Split and assign first part to firstname, assign rest to lastname
            }
            u.role = ConfigurationHelpers.getMagicRoleForUsername(username);
            u.save();
            Activity.LogUserActivity(u, username, ActivityType.CREATED, u.role.name);
        }
        
        session.put(USER_KEY, username);
    }
}
