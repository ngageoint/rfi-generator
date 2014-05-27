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

import org.apache.commons.collections.*;

import flexjson.*;
import flexjson.transformer.*;

public class GroupTests extends UnitTest {
    @Test
    public void testTreeCollection() {
        Group g = Group.findById(2L);
        int gs = g.allDescendentsAndSelf().size();
        Logger.debug("%d", gs);
        assertTrue( gs == 5);
        
        g = Group.findById(1L);
        gs = g.allDescendentsAndSelf().size();
        Logger.debug("%d", gs);
        assertTrue( gs == 7);
        
        Collection<Long> l = Group.collectIds(g.allDescendentsAndSelf());
        CollectionUtils.forAllDo(l, new Closure() {
            public void execute(Object o) {
                Logger.debug("%d", (Long)o);
            }
        });
    }
}
