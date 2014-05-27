update RFIs set country = CONCAT('<',country,'>') where country not like '<%';
update Countries set code = CONCAT('<',code,'>') where code not like '<%';
