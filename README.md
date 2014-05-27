RFI Generator
=============

The RFI (Requests for Information) Generator simplifies the RFI creation and management process and works well within any desktop or tablet browser.  The application has been successfully used by first responders supporting relief efforts during Hurricanes Isaac and Sandy, flooding in Boulder Colorado, mudslides in Snohomish County Washington, and wildfires in Valparaiso Chile.  

The RFI Generator software was developed at the National Geospatial-Intelligence Agency (NGA) in partnership with [NJVC] (http://www.njvc.com/about-us/management-team).  The government has "unlimited rights" and is releasing this software to increase the impact of government investments by providing developers with the opportunity to take things in new directions. The software use, modification, and distribution rights are stipulated within the MIT license.  

###Pull Requests
If you'd like to contribute to this project, please make a pull request. We'll review the pull request and discuss the changes. All pull request contributions to this project will be released under the MIT license.  

Software source code previously released under an open source license and then modified by NGA staff is considered a "joint work" (see 17 USC § 101); it is partially copyrighted, partially public domain, and as a whole is protected by the copyrights of the non-government authors and must be released according to the terms of the original open source license.

###This software uses:
Play! Framework 1.2.4 under Apache 2.0

MySQL under GPL license

Java under GPL license

###Installation Instructions (for a *nix-based host):
•	Clone the rfi_gen repo to your development machine

•	Download and install the Play! Framework (version 1.2.4) at http://downloads.typesafe.com/releases/play-1.2.4.zip

•	After unzipping, add the path to the unzipped location to your .bash_profile or .bashrc

•	Install MySQL (5+), and create a rfi_gen and rfi_gen_test database

•	By default, the root user is used w/ “rfi_gen” password for connections to the aforementioned databases

•	Run “play dependencies” from the root of the code’s download location to download required dependencies

•	Run “./run.sh” to run the RFI Generator

•	Open http://localhost:9000 to view the RFI Generator in your local web browser

##Development Details and Integration

The application consists of two interfaces: a map-based experience as well as an admin experience.

The admin experience is a power-user view into the data that displays RFIs/Event/other domain models in a tabular/grid based page under /admin.
This experience is only for users that are part of the analyst/management roles.  When a user that
is part of the "Analyst" or "Managment" role logs into the site, they are automatically redirected to the
"Admin experience".

The "Map experience" is the map entry point for users that are part of the "Field" role.  This experience is
vastly different from the admin experience and is more focused on spatial capabilities of the RFI generator
than the admin experience.  A large map in the background of the page indicates you are in the map
experience.  Any user that logs into the RFI generator has access to this experience.  If a user in the admin
experience wishes to visit the map experience, clicking the "Go to map view" in the admin experience's main
nav will redirect the user to the map experience.

To submit an RFI in the map experience, simply choose the geospatial tool of choice (from the right-hand tools) and draw the area of interest for the request.  After selecting an area, a popup will display with fields used to house RFI information.  After filling out the required fields, clicking "Create" will create the RFI and notify the associated event's administation that an RFI has been submitted. 

###Screenshots


![First Responder Map Experience](https://cloud.githubusercontent.com/assets/5178768/3094961/f642754c-e5c1-11e3-81dc-91239e26b2d4.png) First Responder Map Experience

![Power User admin-tab experience](https://cloud.githubusercontent.com/assets/5178768/3094960/f64098c6-e5c1-11e3-978d-8af9f9ef331c.png) Power User Admin/Tabular Experience

![New Point-based RFI](https://cloud.githubusercontent.com/assets/5178768/3094963/f655f0a4-e5c1-11e3-888c-a919cc565777.png)Creating a new point-based spatial RFI in the map experience

![Reporting features](https://cloud.githubusercontent.com/assets/5178768/3094962/f64b6166-e5c1-11e3-9ab4-7f9b2d772312.png)Reporting Features

![New Spatial RFI with admin experience](https://cloud.githubusercontent.com/assets/5178768/3094964/f6577d20-e5c1-11e3-8a53-75e4d7f76787.png) Creating a new spatial RFI with the admin experience

### Developers

First thing, read through: http://www.playframework.com/documentation/1.2.4/home
Doing the "Yet Another Blog Engine" tutorial will be invaluable

#### Terms

"Popover":
An enhanced tooltip...used on the models lists as well as form labels, just used to display directional/informative text

"Popup"/"Dialog"/"Modal":
The Twitter bootstrap library supports a modal popup (just called "modal" in their library).  A reference to one
of these terms is probably in reference to the map experience, where basically everything is loaded into
a modal.

"Seed data":
Data that is automatically loaded/used for testing.  All seed data is housed in various YML files under /app/jobs.  If your model changes, or you're testing a specific case, update your seed data.

#### Magic Administrators

Magic usernames that should be admins are included in the conf/magicAdmins.yml file (YAML format).
Upon every user creation (SSO user who doesn't have a user account in the RFI generator yet),
this file will be read and if their uid compared against usernames in the magicAdmins.yml file.
Users who login to the RFI Generator (anyone w/ an iGeoint SSO account) and NOT a member of
this file will be assigned field-level priviledges.

### Setting up for development

Change authClass to "AuthenticationProviders.NoAuth" and set "bypassSecurity" to 1 in the application.conf, you'll have:

    http://localhost:9000/setUser?user=[USERNAME]

route available for setting the username, your development environment will (by default) already be setup to handle this

When you first run the application and the connection to your empty rfi_gen database works, then the schema/seed data will
be automatically imported.  By default, the application will run on http://localhost:9000

#### SQL Migrations

Schema migrations will be handled automatically by the Play! framework.  However, there are often cases where
you want to have a record of what you had to do data-wise in order to support updates to models.

Migrations are stored in conf/migration/*.sql

Putting the file in the format "MAJOR.MINOR.BUILD.sql" is nice in order to track exactly WHEN you
will need to apply this migration to support a progressive development model.  If you're starting off w/ a clean slate,
then you won't need to apply these migrations since the seed data should be up-to-date.

#### GeoQ Integration


[GoeQ] (https://github.com/ngageoint/geoq) integration tasks will fail silently or be ignored completely if no GEOQ related URLs are in the application.conf file.
Pull down the latest GEOQ code and setup the system per the GEOQ install docs (includes installing/setting up geoserver).
Running GeoQ on the default port *should* allow the rfi generator to hook in...ensure you are using
a username/password that GEOQ will recognize (stored in application.conf) w/in the GeoQ settings:

    geoQ.username
    geoQ.password
    geoQ.baseURL

#### OGC Feature Posting

Similar to GeoQ, this feature will fail silently if the RFI Generator is unable to connect to a geoserver.
Setup a layer to house RFIs in geoserver like:

    Workspace:
      Name: rfi
      Namespace: http://rfi.nga.mil
      WFS: enabled
    Datasource:
      Name: rfi
      Expose primary keys: checked
      Whatever you need to get a PostGIS connection setup
    New Layer:
      From: rfi:rfi
      Create feature
        Type name: rfi
        Attributes:
            rfi_id: INTEGER (nullable is fine)
            the_geom: GENERIC GEOMETRY (nullable is fine), EPSG: 4326
      Native bounds are: -180. -90, 180, 90
      Pull lat/long bounding box via "Compute from native bounds"

### Unit/functional tests and testing

Running the project in "test" mode will connect to a completely separate database, named:
"rfi_gen_test", but default.  This database will need to be created (simply creation is
enough, don't need to import any schema).  When test mode is started, all of the seed data
is bootstrapped into the database (see app/job/*.yml files for bootstrapped data)...the "Bootstrap.java"
in this folder will setup the database w/ a decent set of test data.  You must kill/restart (to start w/
appropriate seed data) the server process to get the full-gamut of tests to pass.

Run using ./test.sh

After you launch your browser, hit:

    http://localhost:9000/@tests

To get the test console.  Note: for the GEOQIntegrationTests and OGCTest classes to work, you need to have
GEOQ running on localhost:3000 and an instance of geoserver running on localhost:8080/geoserver (see above).
Also, sometimes the RFICRUD (which tests receiving emails) test might fail.  You can fairly safely ignore this
unit test.

Also, because of the class unloading (I think), sometimes the report generation fails when unit testing.  To fix, just
kill and restart the server via ./test.sh.

You can view the latest test coverage report by hitting:

    http://localhost:9000/@cobertura

The cool thing about the test-coverage report is that you can click through the site to review how much code you've actually hit.

### WAR'ing things up for deployment

    cd utils
    ./deploy.sh
    
This will create a WAR set to run in %prod% mode in /tmp/rfi_gen.war.  During this process,
the build number is automatically generated.
The build number is appended to the MAJOR.MINOR release #.  In order for your build # to update when using
another SCM system, you'll need to update this script for your needs.

