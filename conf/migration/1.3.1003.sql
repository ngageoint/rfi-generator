update ReportingSnapshots set onHoldRFIs = 0;
update ReportingSnapshots set acceptedRFIs = notStartedRFIs;

update RFIs set status = 'Accepted' where status = 'Not Started';
