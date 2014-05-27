import org.junit.*;
import play.*;
import play.test.*;
import play.mvc.*;
import play.mvc.Http.*;
import models.*;
import java.util.*;

public class URLBasicDataPosting extends FunctionalTest {
    @Test
    public void testCreateAdminNote() {
        Map<String,String> note = new HashMap<String,String>() {{
            put("c[text]", "TEST NOTE");
        }};
        assertStatus(200,POST("/admin/createNote", note));
    }
    
    @Test
    public void testCreateAdminComment() {
        Map<String,String> comment = new HashMap<String,String>() {{
            put("c[text]", "TEST COMMENT");
            put("c[rfi][id]", "1");
        }};
        assertStatus(200,POST("/rfi/createadmincomment", comment));
    }
    
    @Test
    public void testCreateUsertCommentFromAdmin() {
        Map<String,String> comment = new HashMap<String,String>() {{
            put("c[text]", "TEST USER COMMENT");
            put("c[rfi][id]", "1");
        }};
        assertStatus(200,POST("/rfi/createcommentjson", comment));
    }
    
    @Test
    public void testCreateUsertCommentFromMap() {
        Map<String,String> comment = new HashMap<String,String>() {{
            put("c.rfi.id", "1");
            put("c.text", "TEST USER COMMENT");
        }};
        assertStatus(200,POST("/rfi/comment", comment));
    }
    
    @Test
    public void testCreateProduct() {
        Map<String,String> rel = new HashMap<String,String>() {{
            put("rel[rfi][id]", "1");
            put("rel[text]", "http://www.google.com <- TEST product");
        }};
        assertStatus(200,POST("/rfi/product", rel));
    }
    
    @Test
    public void testCreateWeblink() {
        Map<String,String> rel = new HashMap<String,String>() {{
            put("rel[rfi][id]", "1");
            put("rel[text]", "http://www.google.com <- TEST weblink");
        }};
        assertStatus(200,POST("/rfi/weblink", rel));
    }
    
    @Test
    public void testDeleteRFI() {
        assertStatus(200,DELETE("/rfi/delete/2"));
    }
    
    @Test
    public void testCreateEvent() {
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
}
