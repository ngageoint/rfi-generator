RFI (Request for Informatin) generator
=============

The RFI Generator simplifies the RFI creation and management process and works well within any desktop or tablet browser.  The application has been successfully used by first responders supporting relief efforts during Hurricanes Isaac and Sandy, flooding in Boulder Colorado, mudslides in Snohomish County Washington, and wildfires in Valparaiso Chile.  

The RFI Generator software was developed at the National Geospatial-Intelligence Agency (NGA) in partnership with [NJVC] (http://www.njvc.com/about-us/management-team).  The government has "unlimited rights" and is releasing this software to increase the impact of government investments by providing developers with the opportunity to take things in new directions. The software use, modification, and distribution rights are stipulated within the MIT license.  

##Pull Requests
If you'd like to contribute to this project, please make a pull request. We'll review the pull request and discuss the changes. All pull request contributions to this project will be released under the MIT license.  

Software source code previously released under an open source license and then modified by NGA staff is considered a "joint work" (see 17 USC § 101); it is partially copyrighted, partially public domain, and as a whole is protected by the copyrights of the non-government authors and must be released according to the terms of the original open source license.

##This software uses:
Play! Framework 1.2.4 under Apache 2 License
MySQL under GPL license
Java under GPL license

##Installation Instructions (for a *nix-based host):
•	Clone the rfi_gen repo to your development machine
•	Download and install the Play! Framework (version 1.2.4) at http://downloads.typesafe.com/releases/play-1.2.4.zip
•	After unzipping, add the path to the unzipped location to your .bash_profile or .bashrc .
•	Install MySQL (5+), and create a rfi_gen and rfi_gen_test database.
•	By default, the root user is used w/ “rfi_gen” password for connections to the aforementioned databases.
•	Run “play dependencies” from the root of the code’s download location to download required dependencies.
•	Run “./run.sh” to run the RFI Generator
•	Open http://localhost:9000 to view the RFI Generator in your local web browser.

##Development Details and Integration
