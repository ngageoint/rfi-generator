package jobs;

import play.*;
import play.jobs.*;
import play.test.*;

import helpers.*;
 
import models.*;

import java.util.List;

@OnApplicationStart
// Not currently used, was used to live migrate RFI's/Event's "networkID" at one point
public class Migrate extends Job {
    public void doJob() {
        if ( Play.configuration.getProperty("migration", "0").equals("1")) {
            List<RFI> rfis = RFI.find("networkID is null or networkID = ''").fetch();
            for ( RFI r:rfis) {
                if ( r.networkID == null || r.networkID.equals("")) {
                    r.networkID = ApplicationHelpers.getIDString(r.id);
                    r.save();
                }
            }
            
            List<Event> events = Event.find("networkID is null or networkID = ''").fetch();
            for ( Event e:events) {
                if ( e.networkID == null || e.networkID.equals("")) {
                    e.networkID = ApplicationHelpers.getIDString(e.id);
                    e.save();
                }
            }
        }
    }
}
