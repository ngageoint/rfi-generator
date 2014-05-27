import org.junit.*;
import play.*;
import play.test.*;
import play.mvc.*;
import play.mvc.Http.*;
import models.*;
import java.util.*;

public class URLGETTests extends FunctionalTest {
    @Test
    public void testThatUserFacingGETsDontCrapOut() {
        // Yeah, this is ghetto, but a good quick way to hit ALL of the GETs
        assertStatus(200,GET("/?noRedirect=true"));
        assertStatus(200,GET("/bi"));
        assertStatus(200,GET("/bi/activities"));
        assertStatus(200,GET("/grid"));
        assertStatus(200,GET("/eventgrid"));
        assertStatus(200,GET("/exportXLS"));
        assertStatus(200,GET("/rfi/edit/1"));
        assertStatus(200,GET("/rfi/edit/1?full=true"));
        assertStatus(200,GET("/rfi/new"));
        assertStatus(200,GET("/rfi/comment/1"));
        assertStatus(200,GET("/rfi/comment/1?full=true"));
        assertStatus(200,GET("/rfi/mygrid"));
        assertStatus(200,GET("/event/new"));
        assertStatus(200,GET("/event/edit/1"));
        assertStatus(200,GET("/eventgrid"));
        assertStatus(200,GET("/updatemyinfo"));
        assertStatus(302,GET("/setUser?user=user"));
        assertStatus(200,GET("/updatemyinfo"));
    }
    
    @Test
    public void testThatAdminGETsDontCrapOut() {
        // This *should* redirect to the dashboard page
        assertStatus(302,GET("/admin"));
        assertStatus(200,GET("/admin/rfis"));
        assertStatus(200,GET("/admin/my_rfis"));
        assertStatus(200,GET("/admin/edit/1"));
        assertStatus(200,GET("/admin/new"));
        assertStatus(200,GET("/admin/importexport"));
        assertStatus(200,GET("/admin/exportSingle?id=1"));
        assertStatus(200,GET("/admin/events"));
        assertStatus(200,GET("/admin/newEvent"));
        assertStatus(200,GET("/admin/editEvent/1"));
        assertStatus(200,GET("/admin/users"));
        assertStatus(200,GET("/admin/editUser/1"));
        assertStatus(200,GET("/admin/reports"));
        assertStatus(200,GET("/admin/dashboard"));
        assertStatus(200,GET("/admin/dashboardOverview"));
        assertStatus(200,GET("/admin/rfiTotals"));
        assertStatus(200,GET("/admin/groups"));
        assertStatus(200,GET("/admin/editGroup/1"));
        assertStatus(200,GET("/admin/newGroup"));
        assertStatus(200,GET("/admin/editMyInfo"));
        
        assertStatus(200,GET("/admin/generate?format=pdf"));
        assertStatus(200,GET("/admin/jfreeTotals/"));
        assertStatus(200,GET("/admin/jfreeByCreated/"));
        assertStatus(200,GET("/admin/jfreeByEvent/"));
        
        assertStatus(200,GET("/admin/byEvent"));
        assertStatus(200,GET("/admin/byCreated"));
        
        assertStatus(200,GET("/user/grid"));
        assertStatus(200,GET("/user/edit?id=1"));
    }
    
    @Test
    public void testThatJSONGETsDontCrapOut() {
        assertStatus(200,GET("/bi/activities"));
        assertStatus(200,GET("/events"));
        assertStatus(200,GET("/event/1.json"));
        assertStatus(200,GET("/rfis"));
        assertStatus(200,GET("/rfi/1.json"));
        // This one poops out quite a bit
        //assertStatus(200,GET("/services/geonames/missouri"));
        assertStatus(200,GET("/services/rfisearch/test"));
        assertStatus(200,GET("/groups"));
    }
}
