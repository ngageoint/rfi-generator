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

import flexjson.*;
import flexjson.transformer.*;

import javax.persistence.*;
import javax.persistence.criteria.*;

import controllers.deadbolt.*;

import org.yaml.snakeyaml.Yaml;
import org.apache.commons.lang.*;

import models.*;

// Used to mixin some nice serialization helpers (rely on response), more tightly
// bound to response context than the SerializationHelpers.java
public abstract class BaseController extends Controller {
    static void serialize(Object o) {
        response.contentType = SerializationHelpers.JSON_CONTENT_TYPE;
        String resp = SerializationHelpers.serialize(o);
        renderText(resp);
    }
    
    static void serialize(Object o, boolean deep) {
        response.contentType = SerializationHelpers.JSON_CONTENT_TYPE;
        renderText(SerializationHelpers.serialize(o, deep));
    }
    
    static void rawSerialize(Object o) {
        response.contentType = SerializationHelpers.JSON_CONTENT_TYPE;
        renderText(SerializationHelpers.rawSerialize(o));
    }
    
    static void rawSerialize(Object o, boolean deep) {
        response.contentType = SerializationHelpers.JSON_CONTENT_TYPE;
        renderText(SerializationHelpers.rawSerialize(o, deep));
    }
    
    protected static void serializeRFIs(Object o) {
        response.contentType = SerializationHelpers.JSON_CONTENT_TYPE;
        JSONSerializer ser = SerializationHelpers.getRFISerializer();
        renderText(ser.serialize(o));
    }
    
    protected static void serializeFullRFIs(Object o) {
        response.contentType = SerializationHelpers.JSON_CONTENT_TYPE;
        JSONSerializer ser = SerializationHelpers.getRFIFullSerializer();
        renderText(ser.serialize(o));
    }
    
    protected static void serializeErrors(Object o) {
        rawSerialize(o, true);
    }
    
    protected static void serializeEvents(Object o) {
        response.contentType = SerializationHelpers.JSON_CONTENT_TYPE;
        JSONSerializer ser = SerializationHelpers.getEventSerializer();
        renderText(ser.serialize(o));
    }
    
    protected static void serializeComments(Object o) {
        rawSerialize(o);
    }
    
    protected static void serializeRelatedItems(Object o) {
        rawSerialize(o);
    }
    
    protected static void serializeGroups(Object o) {
        rawSerialize(o);
    }
}
