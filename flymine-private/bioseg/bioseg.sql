-- Create the user-defined type for 1-D integer intervals (bioseg)
-- 

-- Adjust this setting to control where the objects get created.
SET search_path = public;

CREATE FUNCTION bioseg_in(cstring)
RETURNS bioseg
AS '$libdir/bioseg'
LANGUAGE C STRICT IMMUTABLE;

CREATE FUNCTION bioseg_out(bioseg)
RETURNS cstring
AS '$libdir/bioseg'
LANGUAGE C STRICT IMMUTABLE;

CREATE FUNCTION bioseg_create(int4, int4)
RETURNS bioseg
AS '$libdir/bioseg'
LANGUAGE C STRICT IMMUTABLE;

CREATE TYPE bioseg (
        INTERNALLENGTH = 8,
        INPUT = bioseg_in,
        OUTPUT = bioseg_out
);

COMMENT ON TYPE bioseg IS
'integer point interval ''INT..INT'' or ''INT''';

--
-- External C-functions for R-tree methods
--

-- Left/Right methods

CREATE FUNCTION bioseg_over_left(bioseg, bioseg)
RETURNS bool
AS '$libdir/bioseg'
LANGUAGE C STRICT IMMUTABLE;

COMMENT ON FUNCTION bioseg_over_left(bioseg, bioseg) IS
'overlaps or is left of';

CREATE FUNCTION bioseg_over_right(bioseg, bioseg)
RETURNS bool
AS '$libdir/bioseg'
LANGUAGE C STRICT IMMUTABLE;

COMMENT ON FUNCTION bioseg_over_right(bioseg, bioseg) IS
'overlaps or is right of';

CREATE FUNCTION bioseg_left(bioseg, bioseg)
RETURNS bool
AS '$libdir/bioseg'
LANGUAGE C STRICT IMMUTABLE;

COMMENT ON FUNCTION bioseg_left(bioseg, bioseg) IS
'is left of';

CREATE FUNCTION bioseg_right(bioseg, bioseg)
RETURNS bool
AS '$libdir/bioseg'
LANGUAGE C STRICT IMMUTABLE;

COMMENT ON FUNCTION bioseg_right(bioseg, bioseg) IS
'is right of';


-- Scalar comparison methods

CREATE FUNCTION bioseg_lt(bioseg, bioseg)
RETURNS bool
AS '$libdir/bioseg'
LANGUAGE C STRICT IMMUTABLE;

COMMENT ON FUNCTION bioseg_lt(bioseg, bioseg) IS
'less than';

CREATE FUNCTION bioseg_le(bioseg, bioseg)
RETURNS bool
AS '$libdir/bioseg'
LANGUAGE C STRICT IMMUTABLE;

COMMENT ON FUNCTION bioseg_le(bioseg, bioseg) IS
'less than or equal';

CREATE FUNCTION bioseg_gt(bioseg, bioseg)
RETURNS bool
AS '$libdir/bioseg'
LANGUAGE C STRICT IMMUTABLE;

COMMENT ON FUNCTION bioseg_gt(bioseg, bioseg) IS
'greater than';

CREATE FUNCTION bioseg_ge(bioseg, bioseg)
RETURNS bool
AS '$libdir/bioseg'
LANGUAGE C STRICT IMMUTABLE;

COMMENT ON FUNCTION bioseg_ge(bioseg, bioseg) IS
'greater than or equal';

CREATE FUNCTION bioseg_contains(bioseg, bioseg)
RETURNS bool
AS '$libdir/bioseg'
LANGUAGE C STRICT IMMUTABLE;

COMMENT ON FUNCTION bioseg_contains(bioseg, bioseg) IS
'contains';

CREATE FUNCTION bioseg_contained(bioseg, bioseg)
RETURNS bool
AS '$libdir/bioseg'
LANGUAGE C STRICT IMMUTABLE;

COMMENT ON FUNCTION bioseg_contained(bioseg, bioseg) IS
'contained in';

CREATE FUNCTION bioseg_overlap(bioseg, bioseg)
RETURNS bool
AS '$libdir/bioseg'
LANGUAGE C STRICT IMMUTABLE;

COMMENT ON FUNCTION bioseg_overlap(bioseg, bioseg) IS
'overlaps';

CREATE FUNCTION bioseg_same(bioseg, bioseg)
RETURNS bool
AS '$libdir/bioseg'
LANGUAGE C STRICT IMMUTABLE;

COMMENT ON FUNCTION bioseg_same(bioseg, bioseg) IS
'same as';

CREATE FUNCTION bioseg_different(bioseg, bioseg)
RETURNS bool
AS '$libdir/bioseg'
LANGUAGE C STRICT IMMUTABLE;

COMMENT ON FUNCTION bioseg_different(bioseg, bioseg) IS
'different';

-- support routines for indexing

CREATE OR REPLACE FUNCTION bioseg_cmp(bioseg, bioseg)
RETURNS int4
AS '$libdir/bioseg'
LANGUAGE C STRICT IMMUTABLE;

COMMENT ON FUNCTION bioseg_cmp(bioseg, bioseg) IS 'btree comparison function';

CREATE FUNCTION bioseg_union(bioseg, bioseg)
RETURNS bioseg
AS '$libdir/bioseg'
LANGUAGE C STRICT IMMUTABLE;

CREATE FUNCTION bioseg_inter(bioseg, bioseg)
RETURNS bioseg
AS '$libdir/bioseg'
LANGUAGE C STRICT IMMUTABLE;

CREATE FUNCTION bioseg_size(bioseg)
RETURNS int4
AS '$libdir/bioseg'
LANGUAGE C STRICT IMMUTABLE;

-- miscellaneous

CREATE FUNCTION bioseg_upper(bioseg)
RETURNS int4
AS '$libdir/bioseg'
LANGUAGE C STRICT IMMUTABLE;

CREATE FUNCTION bioseg_lower(bioseg)
RETURNS int4
AS '$libdir/bioseg'
LANGUAGE C STRICT IMMUTABLE;


--
-- OPERATORS
--

CREATE OPERATOR < (
        LEFTARG = bioseg,
        RIGHTARG = bioseg,
        PROCEDURE = bioseg_lt,
        COMMUTATOR = '>',
        NEGATOR = '>=',
        RESTRICT = scalarltsel,
        JOIN = scalarltjoinsel
);

CREATE OPERATOR <= (
        LEFTARG = bioseg,
        RIGHTARG = bioseg,
        PROCEDURE = bioseg_le,
        COMMUTATOR = '>=',
        NEGATOR = '>',
        RESTRICT = scalarltsel,
        JOIN = scalarltjoinsel
);

CREATE OPERATOR > (
        LEFTARG = bioseg,
        RIGHTARG = bioseg,
        PROCEDURE = bioseg_gt,
        COMMUTATOR = '<',
        NEGATOR = '<=',
        RESTRICT = scalargtsel,
        JOIN = scalargtjoinsel
);

CREATE OPERATOR >= (
        LEFTARG = bioseg,
        RIGHTARG = bioseg,
        PROCEDURE = bioseg_ge,
        COMMUTATOR = '<=',
        NEGATOR = '<',
        RESTRICT = scalargtsel,
        JOIN = scalargtjoinsel
);

CREATE OPERATOR << (
        LEFTARG = bioseg,
        RIGHTARG = bioseg,
        PROCEDURE = bioseg_left,
        COMMUTATOR = '>>',
        RESTRICT = positionsel,
        JOIN = positionjoinsel
);

CREATE OPERATOR &< (
        LEFTARG = bioseg,
        RIGHTARG = bioseg,
        PROCEDURE = bioseg_over_left,
        RESTRICT = positionsel,
        JOIN = positionjoinsel
);

CREATE OPERATOR && (
        LEFTARG = bioseg,
        RIGHTARG = bioseg,
        PROCEDURE = bioseg_overlap,
        COMMUTATOR = '&&',
        RESTRICT = areasel,
        JOIN = areajoinsel
);

CREATE OPERATOR &> (
        LEFTARG = bioseg,
        RIGHTARG = bioseg,
        PROCEDURE = bioseg_over_right,
        RESTRICT = positionsel,
        JOIN = positionjoinsel
);

CREATE OPERATOR >> (
        LEFTARG = bioseg,
        RIGHTARG = bioseg,
        PROCEDURE = bioseg_right,
        COMMUTATOR = '<<',
        RESTRICT = positionsel,
        JOIN = positionjoinsel
);

CREATE OPERATOR = (
        LEFTARG = bioseg,
        RIGHTARG = bioseg,
        PROCEDURE = bioseg_same,
        COMMUTATOR = '=',
        NEGATOR = '<>',
        RESTRICT = eqsel,
        JOIN = eqjoinsel,
        MERGES
);

CREATE OPERATOR <> (
        LEFTARG = bioseg,
        RIGHTARG = bioseg,
        PROCEDURE = bioseg_different,
        COMMUTATOR = '<>',
        NEGATOR = '=',
        RESTRICT = neqsel,
        JOIN = neqjoinsel
);

CREATE OPERATOR @> (
        LEFTARG = bioseg,
        RIGHTARG = bioseg,
        PROCEDURE = bioseg_contains,
        COMMUTATOR = '<@',
        RESTRICT = contsel,
        JOIN = contjoinsel
);

CREATE OPERATOR <@ (
        LEFTARG = bioseg,
        RIGHTARG = bioseg,
        PROCEDURE = bioseg_contained,
        COMMUTATOR = '@>',
        RESTRICT = contsel,
        JOIN = contjoinsel
);



-- define the GiST support methods
CREATE FUNCTION gbioseg_consistent(internal,bioseg,int4)
RETURNS bool
AS '$libdir/bioseg'
LANGUAGE C;

CREATE FUNCTION gbioseg_compress(internal)
RETURNS internal 
AS '$libdir/bioseg'
LANGUAGE C;

CREATE FUNCTION gbioseg_decompress(internal)
RETURNS internal 
AS '$libdir/bioseg'
LANGUAGE C;

CREATE FUNCTION gbioseg_penalty(internal,internal,internal)
RETURNS internal
AS '$libdir/bioseg'
LANGUAGE C STRICT;

CREATE FUNCTION gbioseg_picksplit(internal, internal)
RETURNS internal
AS '$libdir/bioseg'
LANGUAGE C;

CREATE FUNCTION gbioseg_union(internal, internal)
RETURNS bioseg 
AS '$libdir/bioseg'
LANGUAGE C;

CREATE FUNCTION gbioseg_same(bioseg, bioseg, internal)
RETURNS internal 
AS '$libdir/bioseg'
LANGUAGE C;


-- Create the operator classes for indexing

CREATE OPERATOR CLASS bioseg_ops
    DEFAULT FOR TYPE bioseg USING btree AS
        OPERATOR        1       < ,
        OPERATOR        2       <= ,
        OPERATOR        3       = ,
        OPERATOR        4       >= ,
        OPERATOR        5       > ,
        FUNCTION        1       bioseg_cmp(bioseg, bioseg);

CREATE OPERATOR CLASS gist_bioseg_ops
DEFAULT FOR TYPE bioseg USING gist 
AS
        OPERATOR        1       << ,
        OPERATOR        2       &< ,
        OPERATOR        3       && ,
        OPERATOR        4       &> ,
        OPERATOR        5       >> ,
        OPERATOR        6       = ,
        OPERATOR        7       @> ,
        OPERATOR        8       <@ ,
        FUNCTION        1       gbioseg_consistent (internal, bioseg, int4),
        FUNCTION        2       gbioseg_union (internal, internal),
        FUNCTION        3       gbioseg_compress (internal),
        FUNCTION        4       gbioseg_decompress (internal),
        FUNCTION        5       gbioseg_penalty (internal, internal, internal),
        FUNCTION        6       gbioseg_picksplit (internal, internal),
        FUNCTION        7       gbioseg_same (bioseg, bioseg, internal);
