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

import org.joda.time.*;
import org.joda.time.format.*;

// See individual helper classes for serializing/deserializing models
// Just a place to house standard serializers/deserializers
public class SerializationHelpers {
    public static String JSON_CONTENT_TYPE = "application/json";

    static JSONSerializer defaultSerializer = new JSONSerializer().
            exclude("*.class").
            include("comments").
            include("attachments").
            exclude("*.fileContent").
            prettyPrint(true);
            
    static JSONSerializer rawSerializer = new JSONSerializer().
            prettyPrint(true);
            
    static JSONSerializer eventSerializer = new JSONSerializer().
            exclude("*.class").
            include("groups").
            include("eventActivities").
            prettyPrint(true);
            
    static JSONDeserializer eventDeserializer = new JSONDeserializer<Event>().
            use(null, Event.class).
            use("eventActivities", new IgnoreObjectFactory()).
            use("groups", new IgnoreObjectFactory()).
            use("createdAt", new IgnoreObjectFactory());
            
    static JSONSerializer rfiSerializer = new JSONSerializer().
            exclude("*.class").
            include("comments").
            include("attachments").
            include("relatedItems").
            exclude("*.fileContent").
            prettyPrint(true);
            
    static JSONSerializer rfiFullSerializer = new JSONSerializer().
            include("comments").
            include("attachments").
            include("internalComments").
            include("relatedItems").
            exclude("*.class").
            transform(new BlobTransformer(), Blob.class).
            prettyPrint(true);
    
    static JSONDeserializer<List<RFI>> rfiFullDeserializer = new JSONDeserializer<List<RFI>>()
            .use("values", RFI.class)
            .use("values.attachments", ArrayList.class)
            .use("values.attachments.values", Attachment.class)
            .use("values.event", Event.class)
            .use("values.eventActivity", EventActivity.class)
            .use("values.comments", ArrayList.class)
            .use("values.comments.values", Comment.class)
            .use("values.internalComments", ArrayList.class)
            .use("values.internalComments.values", AdminComment.class)
            .use("values.relatedItems", ArrayList.class)
            .use("values.relatedItems.values", RelatedRFIItem.class)
            .use("values.coordinates", Point.class);
    
    static {
    }
    
    public static JSONDeserializer getEventDeserializer() {
        return eventDeserializer;
    }
    
    public static JSONSerializer getEventSerializer() {
        return eventSerializer;
    }
    
    public static JSONSerializer getRFISerializer() {
        return rfiSerializer;
    }
    
    public static JSONSerializer getRFIFullSerializer() {
        return rfiFullSerializer;
    }
    
    public static JSONDeserializer<List<RFI>> getRFIFullDeserializer() {
        return rfiFullDeserializer;
    }
    
    public static String rawSerialize(Object o) {
        return rawSerializer.serialize(o);
    }
    
    public static String rawSerialize(Object o, boolean deep) {
        if ( deep) {
            return rawSerializer.deepSerialize(o);
        }
        else {
            return rawSerializer.serialize(o);
        }
    }
    
    public static String serialize(Object o) {
        return defaultSerializer.serialize(o);
    }
    
    public static String serialize(Object o, boolean deep) {
        if ( deep) {
            return defaultSerializer.deepSerialize(o);
        }
        else {
            return defaultSerializer.serialize(o);
        }
    }
}
