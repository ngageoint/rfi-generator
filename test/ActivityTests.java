import org.junit.*;
import java.util.*;
import play.test.*;
import models.*;
import helpers.*;
import javax.*;
import play.*;
import play.db.jpa.*;
import play.mvc.*;

import org.w3c.dom.*;
import org.xml.sax.*;
import java.io.*;
import play.libs.*;

import javax.xml.parsers.*;

import flexjson.*;
import flexjson.transformer.*;

public class ActivityTests extends UnitTest {
    @Test
    public void testNoDuplicatActivities() {
        RFI r = RFI.findById(1L);
        // Test seed data should already include a "created" activity for this RFI, so we *should* only see 1 activity
        // when querying...
        Activity.LogRFIActivity(r, "TEST USER", ActivityType.CREATED);
        assertTrue(Activity.count("rfi = ? and type = ?", r, ActivityType.CREATED) == 1);
    }
}
