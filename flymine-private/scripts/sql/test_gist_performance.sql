\set ECHO all

CREATE TABLE a AS SELECT a FROM generate_series(1,1000000) AS a(a);
CREATE TABLE b AS SELECT b FROM generate_series(1,1000000) AS b(b);

ANALYSE;

CREATE INDEX a_a ON a (a);

EXPLAIN ANALYSE SELECT * FROM a, b WHERE a.a BETWEEN b.b AND b.b + 2;

DROP INDEX a_a;
CREATE INDEX a_a ON a USING gist (a);

EXPLAIN ANALYSE SELECT * FROM a, b WHERE a.a BETWEEN b.b AND b.b + 2;

drop table a ; drop table b;
