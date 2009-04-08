\set ECHO all

-- EXPLAIN ANALYSE SELECT * FROM locatedsequencefeatureoverlappingfeatures l WHERE l.locatedsequencefeature = 71061747;

-- EXPLAIN ANALYSE SELECT * FROM locatedsequencefeatureoverlappingfeatures l WHERE l.locatedsequencefeature BETWEEN 71061747 AND 71061792;

-- EXPLAIN ANALYSE SELECT * FROM locatedsequencefeatureoverlappingfeatures;

EXPLAIN ANALYSE SELECT * FROM locatedsequencefeatureoverlappingfeatures l, gene, primer WHERE l.locatedsequencefeature = gene.id AND l.overlappingfeatures = primer.id;

CREATE INDEX location_object_bioseg ON location USING gist (objectid, bioseg_create(intermine_start, intermine_end));

EXPLAIN ANALYSE SELECT * FROM locatedsequencefeatureoverlappingfeatures l WHERE l.locatedsequencefeature = 71061747;

EXPLAIN ANALYSE SELECT * FROM locatedsequencefeatureoverlappingfeatures l WHERE l.locatedsequencefeature BETWEEN 71061747 AND 71061792;

EXPLAIN ANALYSE SELECT * FROM locatedsequencefeatureoverlappingfeatures;

EXPLAIN ANALYSE SELECT * FROM locatedsequencefeatureoverlappingfeatures l, gene, primer WHERE l.locatedsequencefeature = gene.id AND l.overlappingfeatures = primer.id;
