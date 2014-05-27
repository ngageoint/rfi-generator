import org.junit.*;
import java.util.*;
import play.test.*;
import models.*;
import notifiers.*;
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

// For testing various things, meant to be a transient sandbox for basic stuff
public class Sandbox extends UnitTest {
    @Test
    public void testLoadingMultipleModels() {
        RFI r = RFI.findById(1L);
        Logger.debug(r.toString());
        r.title = "HI, there!";
        RFI.em().detach(r);
        
        Logger.debug(r.title);
        
        RFI r2 = RFI.findById(1L);
        Logger.debug(r2.title);
    }
    
    @Test
    public void testSMTP() {
        RFI r = RFI.findById(3L);
        Mails.rfiCreated(r, "joseph.mctester@njvc.com");
        Mails.commentCreated(r.comments.get(0), "joseph.mctester@njvc.com");
        Mails.eventCreated(r.event, "joseph.mctester@njvc.com");
    }
}
