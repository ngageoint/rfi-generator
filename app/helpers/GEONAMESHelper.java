package helpers;

import flexjson.JSONSerializer;
import flexjson.transformer.*;
import java.util.*;

import play.*;
import play.mvc.*;
import play.utils.*;
import play.libs.*;

import org.w3c.dom.*;

import org.joda.time.*;

import javax.xml.parsers.*;
import java.io.*;

// Some of the geonames interface helpers, mainly responsible for converting XML -> JSON
// NOT CURRENTLY USED
public class GEONAMESHelper
{
    public static JSONSerializer ser = new JSONSerializer();
    static Map<String, String> ns = new HashMap<String, String>();
    static {
        ns.put("gmgml", "http://www.intergraph.com/geomedia/gml");
        ns.put("gml", "http://www.opengis.net/gml");
    }
    
    static
    {
        ser.prettyPrint(true);
    }   
    
    public static String QueryAndTransformToJSON(String searchKey) {
        String gsURL = Play.configuration.getProperty("gns.URL").toString();
        String url = gsURL.replaceAll("\\{TERM\\}", searchKey);
        
        String res = WS.url(url).get().getString();
        Logger.debug("RAW: %s", res);
        
        Map<String, Object> ret = new HashMap<String, Object>();
        List<Map<String, Object>> places = new ArrayList<Map<String,Object>>();
        ret.put("places", places);
        ret.put("totalResultsCount", 0);
        
        try {
            DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
            fact.setNamespaceAware(true);
            DocumentBuilder builder = fact.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(res.getBytes("UTF-8")));
            int cnt = 0;
            for (Node place:XPath.selectNodes("//gmgml:GND1_NAME_INFORMATION", doc, ns)) {
                Map<String,Object> curPlace = new HashMap<String,Object>();
                curPlace.put("name", XPath.selectNode("gmgml:NAME_READING_ORDER", place, ns).getTextContent());
                curPlace.put("country", XPath.selectNode("gmgml:FEATURE_COUNTRY_CD", place, ns).getTextContent());
                curPlace.put("point", XPath.selectNode("gmgml:GEO_LOC//gml:pos", place, ns).getTextContent());
                places.add(curPlace);
                cnt++;
            }
            ret.put("totalResultsCount", cnt);
        }
        catch ( Exception e) {
        }
        
        String strret = ser.deepSerialize(ret);  
        return strret;
    }   
    
    public static String serialize(Object o)
    {
        return ser.serialize(o);
    }
}
