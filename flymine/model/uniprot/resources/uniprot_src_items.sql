CREATE INDEX reference_refid ON reference(refid)
CREATE INDEX referencelist_refids ON referencelist(refids) WHERE name = 'organisms';
CREATE INDEX item_classname ON item(classname);
