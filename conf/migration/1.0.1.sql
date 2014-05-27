insert into Groups_Users (Groups_id, groupManagers_id) select id, groupManager_id from Groups where groupManager_id is not null;
update Groups set groupManager_id = null;
