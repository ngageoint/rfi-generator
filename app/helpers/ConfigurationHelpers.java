package helpers;

import flexjson.*;
import flexjson.transformer.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;

import org.yaml.snakeyaml.Yaml;

import models.*;

import play.*;
import play.vfs.*;
import play.mvc.*;
import play.utils.*;
import play.db.jpa.*;

import org.joda.time.*;
import org.joda.time.format.*;

import org.apache.commons.lang.ArrayUtils;

// Mostly just wrappers around Play!'s configuration stuff
public class ConfigurationHelpers
{
    static String ID_PREFIX = null;
    static Pattern ID_REGEX = null;
    static Boolean attachmentsEnabled = false;
    static List<SelectContainer> states = new ArrayList<SelectContainer>();
    static Map<String,Map> classifications = null;
    
    static {
        ID_PREFIX = Play.configuration.get("idPrefix").toString();
        ID_REGEX = Pattern.compile(Play.configuration.get("idRegex").toString());
        attachmentsEnabled = Boolean.parseBoolean(Play.configuration.get("attachmentsEnabled").toString());
        loadStates();
    }
    
    static void loadStates() {
        String stateContent = VirtualFile.fromRelativePath("conf/states.yml").contentAsString();
        Yaml yaml = new Yaml();
        LinkedHashMap stateMap = (LinkedHashMap)yaml.load(stateContent);
        for ( String s : (String[])stateMap.keySet().toArray(new String []{})) {
            String value = ((LinkedHashMap)stateMap.get(s)).get("value").toString();
            states.add(new SelectContainer(s, value));
        }
    }
    
    public static List<SelectContainer> getStates() {
        return states;
    }
    
    public static Boolean attachmentsEnabled() {
        return attachmentsEnabled;
    }

    public static String getExportDateFormat() {
        return Play.configuration.get("exportDateFormat").toString();
    }
    
    public static String GetBaseURL() {
        return Play.configuration.get("application.baseUrl").toString();
    }
    
    public static Boolean ShowClassifications() {
        return Boolean.parseBoolean(Play.configuration.getProperty("showClassifications", "0"));
    }
    
    public static String DefaultClassificationLevel() {
        return Play.configuration.getProperty("defaultClassification", "Unclassified");
    }
    
    public static String DefaultClassificationString() {
        return Play.configuration.getProperty("eventClassification", "CLASSIFICATION COULD BE UP TO Unclassified//FOUO");
    }
    
    public static String GetSSORedirectURL() {
        String url = Play.configuration.get("ssoLoginURL").toString();
        
        // Is this just a relative controller reference?
        if ( url.matches("^\\w+$")) {
            return String.format("%s/%s", GetBaseURL(), Play.configuration.get("ssoLoginURL"));
        }
        // Or a full URL
        else {
            return Play.configuration.get("ssoLoginURL").toString();
        }
    }
    
    public static String getBuildInfo() {
        VirtualFile f =  VirtualFile.fromRelativePath("conf/build.info");
        String date = "No build date";
        if ( f.exists()) {
            date = f.contentAsString();
        }
        
        String buildNumber = Play.configuration.getProperty("build", "X.X.X");
        
        return String.format("%s.%s", buildNumber, date);
    }
    
    public static Map<String,Map> getClassificationLevels() {
        if ( classifications == null) {
            VirtualFile v = VirtualFile.fromRelativePath("conf/classifications.yml");
            String classificationList = v.contentAsString();
            
            Yaml yaml = new Yaml();
            classifications = (Map<String,Map>)yaml.load(classificationList);
        }
        
        return classifications;
    }
    
    public static ApplicationRole getMagicRoleForUsername(String username) {
        String defaultRole = Play.configuration.getProperty("defaultRole", "Field");
    
        VirtualFile adminFile = VirtualFile.fromRelativePath("conf/magicAdmins.yml");
        String adminsList = adminFile.contentAsString();
        Yaml yaml = new Yaml();
        LinkedHashMap admins = (LinkedHashMap)yaml.load(adminsList);
        if ( admins.keySet().contains(username)) {
            String role = (String)((LinkedHashMap)admins.get(username)).get("role");
            return ApplicationRole.getByName(role);
        }
        else {
            Logger.info("ASSIGNING %s TO %s", username, defaultRole);
            return ApplicationRole.getByName(defaultRole);
        }
    }
    
    public static String [] getProducts() {
        VirtualFile f = VirtualFile.fromRelativePath("conf/products.yml");
        String p = f.contentAsString();
        Yaml yaml = new Yaml();
        List<String> prods = (List<String>)yaml.load(p);
        return prods.toArray(new String[]{});
    }
    
    public static String [] getReceivedVia() {
        VirtualFile f = VirtualFile.fromRelativePath("conf/receivedVia.yml");
        String p = f.contentAsString();
        Yaml yaml = new Yaml();
        List<String> via = (List<String>)yaml.load(p);
        return via.toArray(new String[]{});
    }
    
    public static String [] rfiCreationAdminEmails() {
        VirtualFile f = VirtualFile.fromRelativePath("conf/rfiAlertees.yml");
        String p = f.contentAsString().toLowerCase();
        if ( !p.trim().equals("")) {
            Yaml yaml = new Yaml();
            List<String> emails = (List<String>)yaml.load(p);
            return emails.toArray(new String[]{});
        }
        else {
            return new String[]{};
        }
    }
    
    public static Pattern getIDRegex() {
        return ID_REGEX;
    }
    
    public static String getFeedbackUrl(Http.Request req) {
        String build = Play.configuration.get("build").toString();
        String subject = Play.configuration.get("subject").toString();
        String feedbackURL = Play.configuration.get("feedbackURL").toString();
        return String.format("%s?subject=%s&action=%s&build=%s", feedbackURL, URLEncoder.encode(subject), URLEncoder.encode(req.url), URLEncoder.encode(build));
    }
    
    public static String smtsURL() {
        return Play.configuration.getProperty("SMTS.URL", "");
    }
    
    public static String [] getAgencies() {
        VirtualFile f = VirtualFile.fromRelativePath("conf/organizations.yml");
        String organizations = f.contentAsString();
        Yaml yaml = new Yaml();
        List<String> orgs = (List<String>)yaml.load(organizations);
        return orgs.toArray(new String[]{});
    }
    
    public static boolean reportingJobEnabled() {
        String prop = Play.configuration.getProperty("jobs.reporting.enabled", "1");
        if ( prop.equals("1")) {
            return true;
        }
        else {
            return false;
        }
    }
    
    public static Boolean analyticsEnabled() {
        String prop = Play.configuration.getProperty("analyticsEnabled", "1");
        if ( prop.equals("1")) {
            return true;
        }
        else {
            return false;
        }
    }
    
    public static Boolean ogcPublishingEnabled() {
        String prop = Play.configuration.getProperty("ogcPublishing", "0");
        if ( prop.equals("1")) {
            return true;
        }
        else {
            return false;
        }
    }
    
    public static Boolean isSecurityBypassed() {
        if ( Play.configuration.get("bypassSecurity") != null && Play.configuration.get("bypassSecurity").toString().equals("1")) {
            return true;
        }
        else {
            return false;
        }
    }
}
