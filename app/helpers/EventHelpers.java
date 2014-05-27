package helpers;

import models.*;

import play.*;
import play.data.validation.*;
import play.db.jpa.*;

import java.util.regex.*;
import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import java.text.*;

import flexjson.*;

import org.apache.commons.lang.*;

import javax.persistence.*;
import javax.persistence.criteria.*;

public class EventHelpers {
    public static void handleEventCreated(Event evt, String username) {
        Set<String> created = new HashSet<String>();
        created.addAll(ApplicationHelpers.extractEmails(evt.adminEmailAddresses));
        
        String [] staticAdmins = ConfigurationHelpers.rfiCreationAdminEmails();
        created.addAll(Arrays.asList(staticAdmins));
        
        // Notify the assigned group managers that this event has been created
        if ( evt.groups != null && evt.groups.size() > 0) {
            for ( Group g: evt.groups) {
                if ( g.groupManagers != null) {
                    for ( User gm: g.groupManagers) {
                        if ( gm.email != null && !gm.email.equals("")) {
                            created.add(gm.email.toLowerCase());
                        }
                    }
                }
            }
        }
        
        for ( String s:created) {
            try {
                notifiers.Mails.eventCreated(evt, s);
            } catch ( Exception e) {
                Logger.debug(e.toString());
            }
        }
    }
    
    public static void copyLeft(Event dest, Event src) {
        dest.name = src.name != null ? src.name:dest.name;
        dest.archived = src.archived != null ? src.archived:dest.archived;
        dest.smtsCategory = src.smtsCategory != null ? src.smtsCategory:dest.smtsCategory;
        dest.region = src.region != null ? src.region:dest.region;
        dest.description = src.description != null ? src.description:dest.description;
        dest.eventActivities = src.eventActivities != null ? src.eventActivities:dest.eventActivities;
        dest.adminEmailAddresses = src.adminEmailAddresses != null ? src.adminEmailAddresses:dest.adminEmailAddresses;
        dest.uuid = src.uuid != null ? src.uuid : dest.uuid;
        dest.geoQId = src.geoQId != null ? src.geoQId:dest.geoQId;
        dest.networkID = src.networkID != null ? src.networkID:dest.networkID;
    }
    
    public static void handleEventUpdated(Event evt, String username, Event old) {
        Set<String> updated = new HashSet<String>();
        
        // There IS a better way to do this...
        Set<Long> oldIds = new HashSet<Long>();
        if ( old.groups != null && old.groups.size() > 0) {
            for ( Group g:old.groups) {
                oldIds.add(g.id);
            }
        }
        
        Set<Long> newIds = new HashSet<Long>();
        if ( evt.groups != null && evt.groups.size() > 0) {
            for ( Group g:evt.groups) {
                newIds.add(g.id);
            }
        }
        
        if ( !newIds.equals(oldIds)) {
            // Groups have changed
            for ( Group g:evt.groups) {
                if ( g.groupManagers != null) {
                    for ( User gm: g.groupManagers) {
                        if (gm.email != null && !gm.email.equals("")) {
                            updated.add(gm.email.toLowerCase());
                        }
                    }
                }
            }
            
            updated.addAll(ApplicationHelpers.extractEmails(evt.adminEmailAddresses));
        }
        
        for ( String s:updated) {
            try {
                notifiers.Mails.eventGroupUpdated(evt, s);
            } catch (Exception e) {
                Logger.debug(e.toString());
            }
        }
    }
    
    public static String[] gatherClassificationEmailStrings(Event e) {
        return new String [] { ConfigurationHelpers.DefaultClassificationString(), "============================================================"};
    }
}
