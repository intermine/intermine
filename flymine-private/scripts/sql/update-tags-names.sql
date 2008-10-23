/* Updates tag names of tags used for internal use from old form without 'im:' prefix to tagnames with this prefix  */
update tag set tagname = 'im:favourite' where tagname = 'favourite';
update tag set tagname = 'im:hidden' where tagname = 'hidden';
update tag set tagname = 'im:' || tag.tagname  where tagname like 'aspect:%';
update tag set tagname = 'im:summary' where tagname = 'placement:summary';

