-- Copy the useful bits of the drosdel database to drosdel_clean.
-- Only two tables are copied: drosdel_release4, which contains the element information and 
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
    available BOOLEAN,
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
    confirmedByPCR BOOLEAN,
    confirmedByGenetics BOOLEAN,
    europeanDrosophilaNumber VARCHAR(20)
);

USE drosdel;

INSERT INTO drosdel_clean.element (name, chromosomeName, start, end, orientation, type, subType,
       available)
SELECT name, chr,
       (CASE WHEN orient = 'forward' THEN start ELSE stop END),
       (CASE WHEN orient = 'forward' THEN stop ELSE start END),
       (CASE WHEN orient = 'forward' THEN 1 ELSE -1 END), type, sub_type,
       (CASE WHEN available = 'y' THEN true ELSE false END)
       FROM drosdel_release4 where start is not null and end is not null;

INSERT INTO drosdel_clean.deletion (element1_id, element2_id, chromosomeName, start, end, available,
                                    confirmedByPCR, confirmedByGenetics, europeanDrosophilaNumber)
SELECT (SELECT elementid from drosdel_clean.element where name = name2),
       (SELECT elementid from drosdel_clean.element where name = name1),
       chr, loc2, loc1, (CASE WHEN available = 'y' THEN true ELSE false END), 
       (CASE WHEN confirm = 'y' THEN true ELSE false END),
       (CASE WHEN genetic = 1 THEN true ELSE false END),
       ED_no
FROM drosdel_del_release4 where loc1 is not null and loc2 is not null;
