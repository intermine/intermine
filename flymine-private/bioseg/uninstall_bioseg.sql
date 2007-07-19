SET search_path = public;

DROP OPERATOR CLASS gist_bioseg_ops USING gist;

DROP OPERATOR CLASS bioseg_ops USING btree;

DROP FUNCTION gbioseg_same(bioseg, bioseg, internal);

DROP FUNCTION gbioseg_union(internal, internal);

DROP FUNCTION gbioseg_picksplit(internal, internal);

DROP FUNCTION gbioseg_penalty(internal,internal,internal);

DROP FUNCTION gbioseg_decompress(internal);

DROP FUNCTION gbioseg_compress(internal);

DROP FUNCTION gbioseg_consistent(internal,bioseg,int4);

DROP OPERATOR <@ (bioseg, bioseg);

DROP OPERATOR @> (bioseg, bioseg);

DROP OPERATOR ~ (bioseg, bioseg);

DROP OPERATOR @ (bioseg, bioseg);

DROP OPERATOR <> (bioseg, bioseg);

DROP OPERATOR = (bioseg, bioseg);

DROP OPERATOR >> (bioseg, bioseg);

DROP OPERATOR &> (bioseg, bioseg);

DROP OPERATOR && (bioseg, bioseg);

DROP OPERATOR &< (bioseg, bioseg);

DROP OPERATOR << (bioseg, bioseg);

DROP OPERATOR >= (bioseg, bioseg);

DROP OPERATOR > (bioseg, bioseg);

DROP OPERATOR <= (bioseg, bioseg);

DROP OPERATOR < (bioseg, bioseg);

DROP FUNCTION bioseg_lower(bioseg);

DROP FUNCTION bioseg_upper(bioseg);

DROP FUNCTION bioseg_size(bioseg);

DROP FUNCTION bioseg_inter(bioseg, bioseg);

DROP FUNCTION bioseg_union(bioseg, bioseg);

DROP FUNCTION bioseg_cmp(bioseg, bioseg);

DROP FUNCTION bioseg_different(bioseg, bioseg);

DROP FUNCTION bioseg_same(bioseg, bioseg);

DROP FUNCTION bioseg_overlap(bioseg, bioseg);

DROP FUNCTION bioseg_contained(bioseg, bioseg);

DROP FUNCTION bioseg_contains(bioseg, bioseg);

DROP FUNCTION bioseg_ge(bioseg, bioseg);

DROP FUNCTION bioseg_gt(bioseg, bioseg);

DROP FUNCTION bioseg_le(bioseg, bioseg);

DROP FUNCTION bioseg_lt(bioseg, bioseg);

DROP FUNCTION bioseg_right(bioseg, bioseg);

DROP FUNCTION bioseg_left(bioseg, bioseg);

DROP FUNCTION bioseg_over_right(bioseg, bioseg);

DROP FUNCTION bioseg_over_left(bioseg, bioseg);

DROP TYPE bioseg CASCADE;
