insert into Events_Groups (Events_id, groups_id) select id, group_id from Events where group_id is not null;
update Events set group_id = null;
