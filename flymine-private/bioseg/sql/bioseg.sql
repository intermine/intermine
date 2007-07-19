--
--  Test bioseg datatype
--

--
-- first, define the datatype.  Turn off echoing so that expected file
-- does not depend on contents of bioseg.sql.
--
SET client_min_messages = warning;
\set ECHO none
\i bioseg.sql
\set ECHO all
RESET client_min_messages;

--
-- testing the input and output functions
--

-- Any number
SELECT '1'::bioseg AS bioseg;
SELECT '99999999'::bioseg AS bioseg;


-- Finite intervals
SELECT '1..2'::bioseg AS bioseg;
SELECT '10000000..20000000'::bioseg AS bioseg;

-- invalid input
SELECT ''::bioseg AS bioseg;
SELECT '..10'::bioseg AS bioseg;
SELECT '0..10'::bioseg AS bioseg;
SELECT '0'::bioseg AS bioseg;
SELECT '-10'::bioseg AS bioseg;
SELECT '-10..-1'::bioseg AS bioseg;
SELECT '-10..0'::bioseg AS bioseg;
SELECT '-10..1'::bioseg AS bioseg;
SELECT 'ABC'::bioseg AS bioseg;
SELECT '1ABC'::bioseg AS bioseg;
SELECT '1.'::bioseg AS bioseg;
SELECT '1.....'::bioseg AS bioseg;
SELECT '.1'::bioseg AS bioseg;
SELECT '1..2.'::bioseg AS bioseg;
SELECT '1 e7'::bioseg AS bioseg;
SELECT '1e700'::bioseg AS bioseg;

--
-- testing the  operators
--

-- equality/inequality:
--
SELECT '24..33'::bioseg = '24..33'::bioseg AS bool;
SELECT '24..50'::bioseg != '24..33'::bioseg AS bool;

-- overlap
--
SELECT '2'::bioseg && '2'::bioseg AS bool;
SELECT '2'::bioseg && '2'::bioseg AS bool;
SELECT '2..2'::bioseg && '2..2'::bioseg AS bool;
SELECT '2..2'::bioseg && '2'::bioseg AS bool;
SELECT '2..2'::bioseg && '2'::bioseg AS bool;
SELECT '2..2'::bioseg && '2'::bioseg AS bool;
SELECT '2'::bioseg && '2..2'::bioseg AS bool;
SELECT '2'::bioseg && '2..2'::bioseg AS bool;
SELECT '2'::bioseg && '2..3'::bioseg AS bool;

-- overlap on the left
--
SELECT '2'::bioseg &< '2'::bioseg AS bool;
SELECT '2'::bioseg &< '3'::bioseg AS bool;
SELECT '1..2'::bioseg &< '2'::bioseg AS bool;
SELECT '1..2'::bioseg &< '3'::bioseg AS bool;
SELECT '1..2'::bioseg &< '1..2'::bioseg AS bool;
SELECT '1..2'::bioseg &< '1..3'::bioseg AS bool;
SELECT '1..2'::bioseg &< '2..3'::bioseg AS bool;
SELECT '1..2'::bioseg &< '3..4'::bioseg AS bool;

-- overlap on the right
--
SELECT '2'::bioseg &> '2'::bioseg AS bool;
SELECT '3'::bioseg &> '2'::bioseg AS bool;
SELECT '2'::bioseg &> '1..2'::bioseg AS bool;
SELECT '3'::bioseg &> '1..2'::bioseg AS bool;
SELECT '1..2'::bioseg &> '1..2'::bioseg AS bool;
SELECT '1..3'::bioseg &> '1..3'::bioseg AS bool;
SELECT '2..3'::bioseg &> '1..2'::bioseg AS bool;
SELECT '3..4'::bioseg &> '1..2'::bioseg AS bool;

-- left
--
SELECT '2'::bioseg << '2'::bioseg AS bool;
SELECT '2'::bioseg << '3'::bioseg AS bool;
SELECT '2..2'::bioseg << '2'::bioseg AS bool;
SELECT '2..3'::bioseg << '3'::bioseg AS bool;
SELECT '2..3'::bioseg << '3'::bioseg AS bool;
SELECT '2..3'::bioseg << '2..3'::bioseg AS bool;
SELECT '2..3'::bioseg << '2..4'::bioseg AS bool;
SELECT '2..3'::bioseg << '3..4'::bioseg AS bool;
SELECT '2..3'::bioseg << '3..5'::bioseg AS bool;

-- right
--
SELECT '2'::bioseg >> '2'::bioseg AS bool;
SELECT '3'::bioseg >> '2'::bioseg AS bool;
SELECT '2'::bioseg >> '2..2'::bioseg AS bool;
SELECT '3'::bioseg >> '2..3'::bioseg AS bool;
SELECT '2..3'::bioseg >> '2..3'::bioseg AS bool;
SELECT '3..4'::bioseg >> '2..3'::bioseg AS bool;


-- "contained in" (the left value belongs within the interval specified in the right value):
--
SELECT '2'::bioseg        <@ '2'::bioseg AS bool;
SELECT '2'::bioseg        <@ '2..3'::bioseg AS bool;
SELECT '3'::bioseg        <@ '2..3'::bioseg AS bool;
SELECT '3..4'::bioseg  <@ '2..5'::bioseg AS bool;
SELECT '2..5'::bioseg  <@ '2..5'::bioseg AS bool;

-- "contains" (the left value contains the interval specified in the right value):
--
SELECT '2'::bioseg @> '2'::bioseg AS bool;
SELECT '2..4'::bioseg <@ '3'::bioseg AS bool;
SELECT '3'::bioseg <@ '2..4'::bioseg AS bool;

-- Load some example data and build the index
-- 
CREATE TABLE test_bioseg (s bioseg);

\copy test_bioseg from 'data/test_bioseg.data'

CREATE INDEX test_bioseg_ix ON test_bioseg USING gist (s);
SELECT count(*) FROM test_bioseg WHERE s @> '11..11';

-- Test sorting 
SELECT * FROM test_bioseg WHERE s @> '11..20' GROUP BY s;

DROP TABLE test_bioseg;
