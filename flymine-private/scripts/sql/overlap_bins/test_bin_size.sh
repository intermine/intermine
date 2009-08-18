#!/bin/bash

SIZE=$1

echo "Testing with size $SIZE"

(
echo "\\timing"
echo "create table locationbin$SIZE as SELECT id, objectid, intermine_start, intermine_end, subjecttype, generate_series(intermine_start / $SIZE, intermine_end / $SIZE) AS bin FROM location order by subjecttype, objectid, bin;"
echo "create index locationbin${SIZE}__subjectobjectbin on locationbin$SIZE (subjecttype, objectid, bin);"
echo "create index locationbin${SIZE}__subjecttypeid on locationbin$SIZE (subjecttype, id);"
echo "analyse locationbin$SIZE;"
echo "explain analyse select count(*) FROM (select distinct l1.id AS id1, l1.intermine_start AS start1, l1.intermine_end AS end1, l2.id AS id2, l2.intermine_start as start2, l2.intermine_end AS end2 from locationbin$SIZE l1, locationbin$SIZE l2 where l1.subjecttype = 'GeneFlankingRegion' AND l2.subjecttype = 'Gene' AND l1.objectid = l2.objectid AND l1.bin = l2.bin) AS a WHERE start1 <= end2 AND start2 <= end1;"
echo "select count(*) FROM (select distinct l1.id AS id1, l1.intermine_start AS start1, l1.intermine_end AS end1, l2.id AS id2, l2.intermine_start as start2, l2.intermine_end AS end2 from locationbin$SIZE l1, locationbin$SIZE l2 where l1.subjecttype = 'GeneFlankingRegion' AND l2.subjecttype = 'Gene' AND l1.objectid = l2.objectid AND l1.bin = l2.bin) AS a WHERE start1 <= end2 AND start2 <= end1;"
) | psql modmine_overlap_test
