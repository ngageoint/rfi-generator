insert into Users_Groups (users_id, groups_id) select id, group_id from Users where group_id is not null;
update Users set group_id = null;
