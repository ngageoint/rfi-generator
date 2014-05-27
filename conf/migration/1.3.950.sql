insert into RelatedRFIItems (text, rfi_id, itemType, create_at) select productURLs, id, "PRODUCT" from RFIs where productURLs is not null and productURLs <> '';
update RFIs set productURLs = '';
insert into RelatedRFIItems (text, rfi_id, itemType) select webLink, id, "WEBLINK" from RFIs where webLink is not null and webLink <> '';
update RFIs set webLink = '';
update RelatedRFIItems set created_at = now();
