CREATE INDEX attribute_value ON attribute(intermine_value) WHERE name = 'id';
CREATE INDEX reference_refid ON reference(refid) WHERE name = 'dbReference';
CREATE INDEX referencelist_refids ON referencelist(refids) WHERE name = 'organisms';
