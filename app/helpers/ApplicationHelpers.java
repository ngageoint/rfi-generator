package helpers;

import flexjson.*;
import flexjson.transformer.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import java.lang.reflect.*;
import java.util.concurrent.atomic.*;

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

// Some of this could probably be refactored further, but this is the house for
// helpers that don't fit into another helper
public class ApplicationHelpers
{
    static JSONSerializer ser = new JSONSerializer();
    static JSONDeserializer dser = new JSONDeserializer();
    
    public static final String PENDING = "Pending";
    public static final String ACCEPTED = "Accepted";
    public static final String IN_WORK = "In Work";
    public static final String ON_HOLD = "On Hold";
    public static final String COMPLETED = "Completed";
    public static final String CANCELED = "Canceled";
    public static final String PERSISTENT = "Persistent";
    public static final String WITH_CUSTOMER = "With Customer";
    
    public static final String mailSplit = "\\s*(,| |;)+\\s*";
    static final String EMAIL_PATTERN =
        "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
        + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
    static Pattern email_pattern = Pattern.compile(EMAIL_PATTERN);
    
    public static final int DEFAULT_PAGE_SIZE = 10;
    
    static
    {
        ser.transform(new JodaTransformer(), DateTime.class);
        // This is ghetto, research into include/exclude rules on a per class basis...
        ser.exclude("*.class").exclude("associatedFile").include("comments").include("attachments").exclude("*.fileContent");
        ser.prettyPrint(true);
    }
    
    public static PeriodFormatter getPeriodFormatter() {
        PeriodFormatter b = new PeriodFormatterBuilder()
            .printZeroRarelyLast()
            .appendDays()
            .appendSuffix(" day ", " days ")
            .appendHours()
            .appendSuffix(" hour ", " hours ")
            .appendMinutes()
            .appendSuffix(" minute ", " minutes ")
            .appendSeconds()
            .appendSuffix(" second", " seconds")
            .toFormatter();
        return b;
    }
    
    public static Collection<String> extractEmails(String em) {
        Collection<String> ret = new HashSet<String>();
        for ( String e: em.split(ApplicationHelpers.mailSplit)) {
            if ( !e.trim().equals("")) {
                ret.add(e.toLowerCase().trim());
            }
        }
        return ret;
    }
    
    public static String[] getStatuses() {
        return new String[] {PENDING, ACCEPTED, IN_WORK, ON_HOLD, COMPLETED, CANCELED, PERSISTENT, WITH_CUSTOMER};
    }
    
    public static Boolean refererInRFISite(String ref) {
        String filter = Play.configuration.get("refererFilter").toString();
        return ref.startsWith(filter);
    }
    
    public static String trim(String inp, int len) {
        return inp.replaceAll(String.format("(?<=.{%d})\\b[\\d\\D]*", len), "...");
    }
    
    public static String getIDString(Long id) {
        return String.format(ConfigurationHelpers.ID_PREFIX, id);
    }
    
    // Only pull out a URL from text (strip out the rest of the text)
    public static String extractURL(String inp) {
        String r = "(?<![=\"\\/>])http(s)?://([\\w+?\\.\\w+])+([a-zA-Z0-9\\~\\!\\@\\#\\$\\%\\^\\&amp;\\*\\(\\)_\\-\\=\\+\\\\\\/\\?\\.\\:\\;\\'\\,]*)?";
        Pattern pattern = Pattern.compile(r, Pattern.DOTALL | Pattern.UNIX_LINES | Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(inp);
        if ( matcher.find()) {
            return matcher.group();
        }
        else {
            return "";
        }
    }
    
    // Server-side linkifying (gnarly regex) - converts http://* text in content to HTML anchors
    public static String linkify(String inp) {
        if ( inp != null) {
            String r = "(?<![=\"\\/>])http(s)?://([\\w+?\\.\\w+])+([a-zA-Z0-9\\~\\!\\@\\#\\$\\%\\^\\&amp;\\*\\(\\)_\\-\\=\\+\\\\\\/\\?\\.\\:\\;\\'\\,]*)?";
            Pattern pattern = Pattern.compile(r, Pattern.DOTALL | Pattern.UNIX_LINES | Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(inp);
            inp = matcher.replaceAll("<a href=\"$0\">$0</a>");
            String re = "(?<![=\"\\/>])[a-zA-Z0-9._-]+\\@[a-zA-Z0-9._-]+";
            pattern = Pattern.compile(re, Pattern.DOTALL | Pattern.UNIX_LINES | Pattern.CASE_INSENSITIVE);
            matcher = pattern.matcher(inp);
            return matcher.replaceAll("<a href=\"mailto:$0\">$0</a>");
        }
        else {
            return "";
        }
    }
    
    public static Boolean isValidEmail(String email) {
        return email_pattern.matcher(email).matches();
    }
    
    public static String fullSerialize(Object o) {
        JSONSerializer s = new JSONSerializer();
        s.include("comments");
        s.include("attachments");
        s.exclude("*.class");
        s.transform(new JodaTransformer(), DateTime.class);
        s.transform(new BlobTransformer(), Blob.class);
        s.prettyPrint(true);
        return s.serialize(o);
    }
    
    public static String serializeMap(Object o) {
        JSONSerializer s = new JSONSerializer();
        s.include("rfis");
        s.include("rfis.comments");
        s.exclude("*.class");
        s.prettyPrint(true);
        return s.serialize(o);
    }
    
    public static String serialize(Object o) {
        return ser.serialize(o);
    }
    
    public static String deepSerialize(Object o) {
        return ser.deepSerialize(o);
    }
    
    public static Object deserialize(String s, Class c) {
        return dser.deserializeInto(s, c);
    }
    
    public static Boolean ContainsInList(String list, String value) {
        if ( list == null) {
            return false;
        }
        else {
            return Arrays.asList(list.split(",\\s*")).contains(value);
        }
    }
    
    public static String statusToCssClass(RFI r) {
        if ( r.isOverdue()) {
            return "overdue";
        }
        else if ( r.isActive()) {
            return "inwork";
        }
        else if ( r.isClosed()) {
            return "completed";
        }
        else {
            return r.status.replaceAll("\\s", "").toLowerCase();
        }
    }
    
    public static List<Map<String,String>> getErrors(List<play.data.validation.Error> errs) {
        List<Map<String,String>> ret = new ArrayList<Map<String,String>>();
        
        for ( play.data.validation.Error e:errs) {
            Map<String,String> cur = new HashMap<String,String>();
            cur.put("key", e.getKey());
            cur.put("message", e.message());
            ret.add(cur);
        }
        
        return ret;
    }
    
    public static void setResponseError(Http.Response response) {
        response.setHeader("FormErrors", "1");
    }
    
    public static void collectErrors(List<play.data.validation.Error> errs, Scope.Flash flash) {
        List<String> messages = new ArrayList<String>();
        
        for ( play.data.validation.Error e: errs) {
            messages.add(e.message());
        }
        flash.error(Utils.join(messages, ","));
    }
    
    //TODO: Find ALL of the areas that need to use this vs. inline date formatting...
    public static String formatDate(Date dt) {
        if ( dt != null) {
            SimpleDateFormat fmt = new SimpleDateFormat("MM/dd/yyyy HH:mm Z");
            return fmt.format(dt);
        }
        else {
            return "";
        }
    }
    
    public static String sanitize(String inp) {
        return inp.replace("\n", "<br/>");
    }
    
    public static int sanitizePageSize(Scope.Session session, int pageSize) {
        if ( pageSize <= 0) {
            pageSize = session.contains("pageSize") ? Integer.parseInt(session.get("pageSize")): DEFAULT_PAGE_SIZE;
        }
        session.put("pageSize", pageSize);
        return pageSize;
    }
    
    static Boolean isRFIField(String name) {
        Boolean exists = false;
        if ( name == null) {
            return false;
        }
        for (Field f: RFI.class.getFields()) {
            if ( f.getName().equals(name)) {
                exists = true;
                break;
            }
        }
        if ( name.startsWith("event_")) {
            exists = true;
        }
        return exists;
    }
    
    static boolean validSortDirection(String dir) {
        if ( dir == null) {
            return false;
        }
        return dir.toLowerCase().equals("asc") || dir.toLowerCase().equals("desc");
    }
    
    public static void sanitizeSortInfo(AtomicReference<List<String>> _sort, AtomicReference<List<String>> _sortDirection, int cnt) {
        List<String> sort = _sort.get();
        List<String> sortDirection = _sortDirection.get();
        if ( sort == null) {
            sort = new ArrayList<String>(cnt);
        }
        if ( sortDirection == null) {
            sortDirection = new ArrayList<String>(cnt);
        }
        while ( sort.size() < cnt) {
            sort.add("createdAt");
        }
        while ( sortDirection.size() < cnt) {
            sortDirection.add("desc");
        }
        for ( String s:sort) {
            if ( !isRFIField(s)) {
                s = "createdAt";
            }
        }
        for ( String s:sortDirection) {
            if ( !validSortDirection(s)) {
                s = "desc";
            }
        }
        _sort.set(sort);
        _sortDirection.set(sortDirection);
    }
}
