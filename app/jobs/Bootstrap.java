package jobs;

import play.*;
import play.jobs.*;
import play.test.*;

import models.*;

@OnApplicationStart
// Loads the initial dataset for the database (seed data contained in yaml files alongside this)
public class Bootstrap extends Job {
    public void doJob() {
        Logger.info(String.format("MODE: %s", Play.configuration.get("application.mode")));
        // Check if the database is empty
        if(Country.count() == 0 &&
            Event.count() == 0 &&
            RFI.count() == 0 &&
            ApplicationRole.count() == 0 &&
            User.count() == 0 &&
            Comment.count() == 0 &&
            Activity.count() == 0 ) {
            
            Logger.info("Bootstrapping/migrating database ddl...");
            Fixtures.deleteDatabase();
            Fixtures.loadModels("jobs/initial-countries.yml");
            Fixtures.loadModels("jobs/initial-foreignCountry.yml");
            Fixtures.loadModels("jobs/initial-groups.yml", "jobs/initial-eventActivities.yml", "jobs/initial-events.yml",
                "jobs/initial-rfis.yml","jobs/initial-applicationRoles.yml","jobs/initial-users.yml",
                "jobs/initial-comments.yml","jobs/initial-activities.yml", "jobs/initial-reportingsnapshots.yml",
                "jobs/initial-relatedItems.yml", "jobs/initial-adminComments.yml");
        }
        
        if ( Group.count() == 0) {
            Fixtures.loadModels("jobs/initial-groups.yml");
        }
    }
}
