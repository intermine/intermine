-- Create the user-defined type for 1-D integer intervals (bioseg)
--

-- Adjust this setting to control where the objects get created.
SET search_path = public;

CREATE FUNCTION bioseg0_in(cstring)
RETURNS bioseg0
AS '$libdir/bioseg0'
LANGUAGE C STRICT IMMUTABLE;

CREATE FUNCTION bioseg0_out(bioseg0)
RETURNS cstring
AS '$libdir/bioseg0'
LANGUAGE C STRICT IMMUTABLE;

CREATE FUNCTION bioseg0_create(int4, int4)
RETURNS bioseg0
AS '$libdir/bioseg0'
LANGUAGE C STRICT IMMUTABLE;

CREATE TYPE bioseg0 (
        INTERNALLENGTH = 8,
        INPUT = bioseg0_in,
        OUTPUT = bioseg0_out
);

COMMENT ON TYPE bioseg0 IS
'integer point interval ''INT..INT'' or ''INT''';

--
-- External C-functions for R-tree methods
--

-- Left/Right methods

CREATE FUNCTION bioseg0_over_left(bioseg0, bioseg0)
RETURNS bool
AS '$libdir/bioseg0'
LANGUAGE C STRICT IMMUTABLE;

COMMENT ON FUNCTION bioseg0_over_left(bioseg0, bioseg0) IS
'overlaps or is left of';

CREATE FUNCTION bioseg0_over_right(bioseg0, bioseg0)
RETURNS bool
AS '$libdir/bioseg0'
LANGUAGE C STRICT IMMUTABLE;

COMMENT ON FUNCTION bioseg0_over_right(bioseg0, bioseg0) IS
'overlaps or is right of';

CREATE FUNCTION bioseg0_left(bioseg0, bioseg0)
RETURNS bool
AS '$libdir/bioseg0'
LANGUAGE C STRICT IMMUTABLE;

COMMENT ON FUNCTION bioseg0_left(bioseg0, bioseg0) IS
'is left of';

CREATE FUNCTION bioseg0_right(bioseg0, bioseg0)
RETURNS bool
AS '$libdir/bioseg0'
LANGUAGE C STRICT IMMUTABLE;

COMMENT ON FUNCTION bioseg0_right(bioseg0, bioseg0) IS
'is right of';


-- Scalar comparison methods

CREATE FUNCTION bioseg0_lt(bioseg0, bioseg0)
RETURNS bool
AS '$libdir/bioseg0'
LANGUAGE C STRICT IMMUTABLE;

COMMENT ON FUNCTION bioseg0_lt(bioseg0, bioseg0) IS
'less than';

CREATE FUNCTION bioseg0_le(bioseg0, bioseg0)
RETURNS bool
AS '$libdir/bioseg0'
LANGUAGE C STRICT IMMUTABLE;

COMMENT ON FUNCTION bioseg0_le(bioseg0, bioseg0) IS
'less than or equal';

CREATE FUNCTION bioseg0_gt(bioseg0, bioseg0)
RETURNS bool
AS '$libdir/bioseg0'
LANGUAGE C STRICT IMMUTABLE;

COMMENT ON FUNCTION bioseg0_gt(bioseg0, bioseg0) IS
'greater than';

CREATE FUNCTION bioseg0_ge(bioseg0, bioseg0)
RETURNS bool
AS '$libdir/bioseg0'
LANGUAGE C STRICT IMMUTABLE;

COMMENT ON FUNCTION bioseg0_ge(bioseg0, bioseg0) IS
'greater than or equal';

CREATE FUNCTION bioseg0_contains(bioseg0, bioseg0)
RETURNS bool
AS '$libdir/bioseg0'
LANGUAGE C STRICT IMMUTABLE;

COMMENT ON FUNCTION bioseg0_contains(bioseg0, bioseg0) IS
'contains';

CREATE FUNCTION bioseg0_contained(bioseg0, bioseg0)
RETURNS bool
AS '$libdir/bioseg0'
LANGUAGE C STRICT IMMUTABLE;

COMMENT ON FUNCTION bioseg0_contained(bioseg0, bioseg0) IS
'contained in';

CREATE FUNCTION bioseg0_overlap(bioseg0, bioseg0)
RETURNS bool
AS '$libdir/bioseg0'
LANGUAGE C STRICT IMMUTABLE;

COMMENT ON FUNCTION bioseg0_overlap(bioseg0, bioseg0) IS
'overlaps';

CREATE FUNCTION bioseg0_same(bioseg0, bioseg0)
RETURNS bool
AS '$libdir/bioseg0'
LANGUAGE C STRICT IMMUTABLE;

COMMENT ON FUNCTION bioseg0_same(bioseg0, bioseg0) IS
'same as';

CREATE FUNCTION bioseg0_different(bioseg0, bioseg0)
RETURNS bool
AS '$libdir/bioseg0'
LANGUAGE C STRICT IMMUTABLE;

COMMENT ON FUNCTION bioseg0_different(bioseg0, bioseg0) IS
'different';

-- support routines for indexing

CREATE OR REPLACE FUNCTION bioseg0_cmp(bioseg0, bioseg0)
RETURNS int4
AS '$libdir/bioseg0'
LANGUAGE C STRICT IMMUTABLE;

COMMENT ON FUNCTION bioseg0_cmp(bioseg0, bioseg0) IS 'btree comparison function';

CREATE FUNCTION bioseg0_size(bioseg0)
RETURNS int4
AS '$libdir/bioseg0'
LANGUAGE C STRICT IMMUTABLE;

-- miscellaneous

CREATE FUNCTION bioseg0_upper(bioseg0)
RETURNS int4
AS '$libdir/bioseg0'
LANGUAGE C STRICT IMMUTABLE;

CREATE FUNCTION bioseg0_lower(bioseg0)
RETURNS int4
AS '$libdir/bioseg0'
LANGUAGE C STRICT IMMUTABLE;

CREATE FUNCTION bioseg0_sel(internal, oid, internal, integer)
RETURNS float
AS '$libdir/bioseg0'
LANGUAGE C STRICT IMMUTABLE;

CREATE FUNCTION bioseg0_joinsel(internal, oid, internal, smallint)
RETURNS FLOAT
AS '$libdir/bioseg0'
LANGUAGE C STRICT IMMUTABLE;

CREATE FUNCTION bioseg0_contsel(internal, oid, internal, integer)
RETURNS float
AS '$libdir/bioseg0'
LANGUAGE C STRICT IMMUTABLE;

CREATE FUNCTION bioseg0_contjoinsel(internal, oid, internal, smallint)
RETURNS FLOAT
AS '$libdir/bioseg0'
LANGUAGE C STRICT IMMUTABLE;


--
-- OPERATORS
--

CREATE OPERATOR < (
        LEFTARG = bioseg0,
        RIGHTARG = bioseg0,
        PROCEDURE = bioseg0_lt,
        COMMUTATOR = '>',
        NEGATOR = '>=',
        RESTRICT = scalarltsel,
        JOIN = scalarltjoinsel
);

CREATE OPERATOR <= (
        LEFTARG = bioseg0,
        RIGHTARG = bioseg0,
        PROCEDURE = bioseg0_le,
        COMMUTATOR = '>=',
        NEGATOR = '>',
        RESTRICT = scalarltsel,
        JOIN = scalarltjoinsel
);

CREATE OPERATOR > (
        LEFTARG = bioseg0,
        RIGHTARG = bioseg0,
        PROCEDURE = bioseg0_gt,
        COMMUTATOR = '<',
        NEGATOR = '<=',
        RESTRICT = scalargtsel,
        JOIN = scalargtjoinsel
);

CREATE OPERATOR >= (
        LEFTARG = bioseg0,
        RIGHTARG = bioseg0,
        PROCEDURE = bioseg0_ge,
        COMMUTATOR = '<=',
        NEGATOR = '<',
        RESTRICT = scalargtsel,
        JOIN = scalargtjoinsel
);

CREATE OPERATOR << (
        LEFTARG = bioseg0,
        RIGHTARG = bioseg0,
        PROCEDURE = bioseg0_left,
        COMMUTATOR = '>>',
        RESTRICT = positionsel,
        JOIN = positionjoinsel
);

CREATE OPERATOR &< (
        LEFTARG = bioseg0,
        RIGHTARG = bioseg0,
        PROCEDURE = bioseg0_over_left,
        RESTRICT = positionsel,
        JOIN = positionjoinsel
);

CREATE OPERATOR && (
        LEFTARG = bioseg0,
        RIGHTARG = bioseg0,
        PROCEDURE = bioseg0_overlap,
        COMMUTATOR = '&&',
        RESTRICT = bioseg0_sel,
        JOIN = bioseg0_joinsel
);

CREATE OPERATOR &> (
        LEFTARG = bioseg0,
        RIGHTARG = bioseg0,
        PROCEDURE = bioseg0_over_right,
        RESTRICT = positionsel,
        JOIN = positionjoinsel
);

CREATE OPERATOR >> (
        LEFTARG = bioseg0,
        RIGHTARG = bioseg0,
        PROCEDURE = bioseg0_right,
        COMMUTATOR = '<<',
        RESTRICT = positionsel,
        JOIN = positionjoinsel
);

CREATE OPERATOR = (
        LEFTARG = bioseg0,
        RIGHTARG = bioseg0,
        PROCEDURE = bioseg0_same,
        COMMUTATOR = '=',
        NEGATOR = '<>',
        RESTRICT = eqsel,
        JOIN = eqjoinsel,
        MERGES
);

CREATE OPERATOR <> (
        LEFTARG = bioseg0,
        RIGHTARG = bioseg0,
        PROCEDURE = bioseg0_different,
        COMMUTATOR = '<>',
        NEGATOR = '=',
        RESTRICT = neqsel,
        JOIN = neqjoinsel
);

CREATE OPERATOR @> (
        LEFTARG = bioseg0,
        RIGHTARG = bioseg0,
        PROCEDURE = bioseg0_contains,
        COMMUTATOR = '<@',
        RESTRICT = bioseg0_contsel,
        JOIN = bioseg0_contjoinsel
);

CREATE OPERATOR <@ (
        LEFTARG = bioseg0,
        RIGHTARG = bioseg0,
        PROCEDURE = bioseg0_contained,
        COMMUTATOR = '@>',
        RESTRICT = bioseg0_contsel,
        JOIN = bioseg0_contjoinsel
);



-- define the GiST support methods
CREATE FUNCTION bioseg0_gist_consistent(internal,bioseg0,int4)
RETURNS bool
AS '$libdir/bioseg0'
LANGUAGE C;

CREATE FUNCTION bioseg0_gist_compress(internal)
RETURNS internal
AS '$libdir/bioseg0'
LANGUAGE C;

CREATE FUNCTION bioseg0_gist_decompress(internal)
RETURNS internal
AS '$libdir/bioseg0'
LANGUAGE C;

CREATE FUNCTION bioseg0_gist_penalty(internal,internal,internal)
RETURNS internal
AS '$libdir/bioseg0'
LANGUAGE C STRICT;

CREATE FUNCTION bioseg0_gist_picksplit(internal, internal)
RETURNS internal
AS '$libdir/bioseg0'
LANGUAGE C;

CREATE FUNCTION bioseg0_gist_union(internal, internal)
RETURNS bioseg0
AS '$libdir/bioseg0'
LANGUAGE C;

CREATE FUNCTION bioseg0_gist_same(bioseg0, bioseg0, internal)
RETURNS internal
AS '$libdir/bioseg0'
LANGUAGE C;


-- Create the operator classes for indexing

CREATE OPERATOR CLASS bioseg0_ops
    DEFAULT FOR TYPE bioseg0 USING btree AS
        OPERATOR        1       < ,
        OPERATOR        2       <= ,
        OPERATOR        3       = ,
        OPERATOR        4       >= ,
        OPERATOR        5       > ,
        FUNCTION        1       bioseg0_cmp(bioseg0, bioseg0);

CREATE OPERATOR CLASS gist_bioseg0_ops
DEFAULT FOR TYPE bioseg0 USING gist
AS
        OPERATOR        1       << ,
        OPERATOR        2       &< ,
        OPERATOR        3       && ,
        OPERATOR        4       &> ,
        OPERATOR        5       >> ,
        OPERATOR        6       = ,
        OPERATOR        7       @> ,
        OPERATOR        8       <@ ,
        FUNCTION        1       bioseg0_gist_consistent (internal, bioseg0, int4),
        FUNCTION        2       bioseg0_gist_union (internal, internal),
        FUNCTION        3       bioseg0_gist_compress (internal),
        FUNCTION        4       bioseg0_gist_decompress (internal),
        FUNCTION        5       bioseg0_gist_penalty (internal, internal, internal),
        FUNCTION        6       bioseg0_gist_picksplit (internal, internal),
        FUNCTION        7       bioseg0_gist_same (bioseg0, bioseg0, internal);
