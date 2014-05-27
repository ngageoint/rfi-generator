update Users u set firstName = trim(substring(fullName, 1, length(fullName) - length(substring_index(fullName, ' ', -1)))), lastName = trim(substring_index(fullName, ' ', -1)) where fullName is not null;

update RFIs r set requestorFirstName = trim(substring(requestor, 1, length(requestor) - length(substring_index(requestor, ' ', -1)))), requestorLastName = trim(substring_index(requestor, ' ', -1)) where r.requestor is not null

update RFIs set requestor = '';
update Users set fullName = '';
-- update Users set firstName = '' where firstName is null
-- update Users set lastName = '' where lastName is null
-- update RFIs set requestorFirstName = '' where requestorFirstName is null;
-- update RFIs set requewstorLastName = '' where requestorLastName is null;
