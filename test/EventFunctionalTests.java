import org.junit.*;
import play.*;
import play.test.*;
import play.mvc.*;
import play.mvc.Http.*;
import models.*;
import java.util.*;
import helpers.*;

public class EventFunctionalTests extends FunctionalTest {
    @Test
    public void testCreateAdminEvent() {
        // Yes, this is usually POST'd via multipart/form-data, but www-form-urlencoded should work, too
        Map<String,String> rel = new HashMap<String,String>() {{
            put("e[id]", "");
            put("e[region]", "[{\"lng\":-105.9079453125,\"lat\":51.368359375},{\"lng\":-103.0075546875,\"lat\":51.45625},{\"lng\":-103.0075546875,\"lat\":50.57734375},{\"lng\":-105.7321640625,\"lat\":50.489453125},{\"lng\":-109.1598984375,\"lat\":49.962109375}]");
            put("e[name]", "Automated test event");
            put("e[smtsCategory]", "");
            put("e[groups][id]", "1");
            put("e[geoQId]", "");
            put("e[adminEmailAddresses", "joe@test.com; test@test.com");
            put("e[description]", "Description for automated test event");
            put("e[archived]", "0");
            put("createdGeoQProject", "0");
            put("newActivity[name]", "New Activitiy via test");
        }};
        // Should redirect back to the list
        Http.Response resp = POST("/admin/updateEvent", rel);
        assertStatus(302, resp);
        assertEquals("/admin/events?pageSize=0&page=0", resp.getHeader("Location"));
    }
    
    @Test
    public void testUpdateAdminEvent() {
        // Yes, this is usually POST'd via multipart/form-data, but www-form-urlencoded should work, too
        Map<String,String> rel = new HashMap<String,String>() {{
            put("e[id]", "5");
            put("e[region]", "[{\"lng\":-105.9079453125,\"lat\":51.368359375},{\"lng\":-103.0075546875,\"lat\":51.45625},{\"lng\":-103.0075546875,\"lat\":50.57734375},{\"lng\":-105.7321640625,\"lat\":50.489453125},{\"lng\":-109.1598984375,\"lat\":49.962109375}]");
            put("e[name]", "Automated test event - after update");
            put("e[smtsCategory]", "");
            put("e[groups][id]", "1");
            put("e[geoQId]", "");
            put("e[adminEmailAddresses", "joe@test.com; test@test.com");
            put("e[description]", "Description for automated test event");
            put("e[archived]", "0");
            put("createdGeoQProject", "0");
        }};
        // Should redirect back to the list
        Http.Response resp = POST("/admin/updateEvent", rel);
        assertStatus(302, resp);
        assertEquals("/admin/events?pageSize=0&page=0", resp.getHeader("Location"));
        
        Http.Response gResp = GET("/event/5.json");
        String eventPayload = gResp.out.toString();
        Event evt5 = (Event)SerializationHelpers.getEventDeserializer().deserialize(eventPayload);
        assertEquals(evt5.name, "Automated test event - after update");
        
        // Attach to JPA context
        ((Event)Event.findById(5L)).delete();
    }
}
