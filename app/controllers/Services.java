package controllers;

import play.*;
import play.libs.*;
import play.db.jpa.*;
import play.mvc.*;

import helpers.*;

import java.util.*;

import javax.persistence.*;
import javax.persistence.criteria.*;

import controllers.deadbolt.*;

import models.*;

import org.joda.time.*;

import flexjson.JSONSerializer;
import flexjson.transformer.*;

import com.google.gson.*;

import javax.xml.parsers.*;
import java.io.*;

// Just used to proxy geonames (no longer used) as well as provide a JSON endpoint
// for RFI searching.
@With({Master.class, Deadbolt.class})
public class Services extends BaseController {
    // NOT USED ANYMORE - no geonames searching available from front-end
    // Work-around for IE8's XDomainRequest...which HAS to have the same scheme
    // as the hosting page to work...since GEONAMES doesn't support HTTPS w/o
    // a subscription, we are proxying through the server side
    public static void geonames(String searchKey) {
        String ret = GEONAMESHelper.QueryAndTransformToJSON(searchKey);
        response.contentType = "application/json";
        renderText(ret);
    }
    
    // JSON endpoint - search for RFI
    public static void rfiSearch(String searchKey, Long filter) throws Exception {
        String searchStr = String.format("%%%s%%", searchKey);
        List<RFI> rfis = null;
        
        Map<String, Object> ret = RFIHelpers.filter(searchKey, filter);
        serialize(ret.get("rfis"));
    }
}
