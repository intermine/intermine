-- Copy the useful bits of the drosdel database to drosdel_clean.
-- Only two tables are copied drosdel_release4, which contains the element information and 
-- drosdel_del_release4, which contains information about the deletions

USE drosdel_clean;

DROP TABLE IF EXISTS deletion;
DROP TABLE IF EXISTS element;

CREATE TABLE element (
    elementid INTEGER auto_increment,
    name VARCHAR(20),
    chromosomeName VARCHAR(20),
    start INTEGER,
    end INTEGER,
    orientation INTEGER,
    type VARCHAR(20),
    subType VARCHAR(20),
    PRIMARY KEY (elementid)
);

CREATE INDEX element_name_index on element(name);

CREATE TABLE deletion (
    element1_id INTEGER,
    element2_id INTEGER,
    chromosomeName VARCHAR(20),
    start INTEGER,
    end INTEGER,
    available BOOLEAN,
    europeanDrosophilaNumber VARCHAR(20)
);

USE drosdel;

INSERT INTO drosdel_clean.element (name, chromosomeName, start, end, orientation, type, subType)
SELECT name, chr,
       (CASE WHEN orient = 'forward' THEN start - 1 ELSE start END),
       (CASE WHEN orient = 'forward' THEN start ELSE start + 1 END),
       (CASE WHEN orient = 'forward' THEN 1 ELSE -1 END), type, sub_type
       FROM drosdel_release4 where start is not null and end is not null;

INSERT INTO drosdel_clean.deletion (element1_id, element2_id, chromosomeName, start, end, available,
                                    europeanDrosophilaNumber)
SELECT (SELECT elementid from drosdel_clean.element where name = name2),
       (SELECT elementid from drosdel_clean.element where name = name1),
       chr, loc2, loc1, (CASE WHEN available = 'y' THEN true ELSE false END), ED_no
FROM drosdel_del_release4 where loc1 is not null and loc-2 is not null;
