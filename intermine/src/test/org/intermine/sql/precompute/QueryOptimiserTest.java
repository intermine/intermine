package org.flymine.sql.precompute;

import junit.framework.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.flymine.sql.query.*;

public class QueryOptimiserTest extends TestCase
{
    private Query singleTableQuery, singleTableQueryWithFieldAlias;
    private Query singleTableQueryWithTableAlias, singleTableQueryNoConstraints;
    private Query twoSameTableQuery;

    public QueryOptimiserTest(String arg1) {
        super(arg1);
    }

    public void setUp() throws Exception {
        singleTableQuery = new Query("SELECT mytable.a FROM mytable WHERE mytable.a = 1");
        singleTableQueryWithFieldAlias = new Query("SELECT mytable.a AS alias FROM mytable WHERE mytable.a = 1");
        singleTableQueryWithTableAlias = new Query("SELECT table1.a FROM mytable table1 WHERE table1.a = 1");
        singleTableQueryNoConstraints = new Query("SELECT mytable.a FROM mytable");
        twoSameTableQuery = new Query("SELECT table1.b, table2.a FROM mytable table1, mytable table2 WHERE table1.a = 1 AND table2.b < 3 AND table1.a = table2.join");
    }

    /*
    public void testMergeNullPrecomputed() throws Exception {
        try {
            QueryOptimiser.merge(null, new Query());
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testMergeNullQuery() throws Exception {
        try {
            QueryOptimiser.merge(new PrecomputedTable(new Query(), "precomp"), null);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testMergeExactMatch() throws Exception {

        PrecomputedTable pt = new PrecomputedTable(singleTableQuery, "precomp1");

        Query q = new Query();
        Table t = new Table("precomp1");
        Field f = new Field("a", t);
        SelectValue sv = new SelectValue(f, null);
        q.addFrom(t);
        q.addSelect(sv);

        Set results = QueryOptimiser.merge(pt, singleTableQuery);

        assertEquals(1, results.size());
        assertEquals(q, (Query) (results.iterator().next()));
    }

    public void testMergeFieldNamesDifferentInPrecomputed() throws Exception {
        PrecomputedTable pt = new PrecomputedTable(singleTableQuery, "precomp1");

        Query q = new Query();
        Table t = new Table("precomp1");
        Field f = new Field("f_alias", t);
        SelectValue sv = new SelectValue(f, "a");
        q.addFrom(t);
        q.addSelect(sv);

        Set results = QueryOptimiser.merge(pt, singleTableQueryWithFieldAlias);

        assertEquals(1, results.size());
        assertEquals(q, (Query) (results.iterator().next()));
    }

    public void testMergeConstraintStillNeeded() throws Exception {
        PrecomputedTable pt = new PrecomputedTable(singleTableQueryNoConstraints, "precomp1");

        Query q = new Query();
        Table t = new Table("precomp1");
        Constant c = new Constant("1");
        Field f = new Field("a", t);
        SelectValue sv = new SelectValue(f, "a");
        q.addFrom(t);
        q.addSelect(sv);
        q.addWhere(new Constraint(f, Constraint.EQ, c));

        Set results = QueryOptimiser.merge(pt, singleTableQuery);

        assertEquals(1, results.size());
        assertEquals(q, (Query) (results.iterator().next()));
    }

    public void testMergeTwoSameTablesInPrecomputed() throws Exception {
        PrecomputedTable pt = new PrecomputedTable(singleTableQueryNoConstraints, "precomp1");

        Query q1 = new Query();
        Table t1 = new Table("precomp1");
        Table t2 = new Table("mytable");
        Constant c = new Constant("1");
        Field f1 = new Field("a", t1);
        Field f2 = new Field("b", t2);
        Field f3 = new Field("join", t2);
        SelectValue sv1 = new SelectValue(f1, "a");
        SelectValue sv2 = new SelectValue(f2, null);
        q1.addFrom(t1);
        q1.addFrom(t2);
        q1.addSelect(sv1);
        q1.addSelect(sv2);
        q1.addWhere(new Constraint(f1, Constraint.EQ, c));
        q1.addWhere(new Constraint(f2, Constraint.LT, c));
        q1.addWhere(new Constraint(f1, Constraint.EQ, f3));

        Query q2 = new Query();
        t1 = new Table("mytable");
        t2 = new Table("precomp1");
        c = new Constant("1");
        f1 = new Field("a", t1);
        f2 = new Field("b", t2);
        f3 = new Field("join", t2);
        sv1 = new SelectValue(f1, null);
        sv2 = new SelectValue(f2, "b");
        q2.addFrom(t1);
        q2.addFrom(t2);
        q2.addSelect(sv1);
        q2.addSelect(sv2);
        q2.addWhere(new Constraint(f1, Constraint.EQ, c));
        q2.addWhere(new Constraint(f2, Constraint.LT, c));
        q2.addWhere(new Constraint(f1, Constraint.EQ, f3));

        // We are expecting 2 results here:
        // 1: t1 replaced with precomp1
        // 2: t2 replaced with precomp1

        Set results = QueryOptimiser.merge(pt, singleTableQuery);
        assertEquals(2, results.size());

        Query first = (Query) (results.iterator().next());
        Query second = (Query) (results.iterator().next());

        assertTrue(!(first.equals(second)));
        assertTrue(q1.equals(first) || q2.equals(first));
        assertTrue(q1.equals(second) || q2.equals(second));
    }

    public void testMergeTwoDifferentTablesInPrecomputed() throws Exception {
    }

    public void testMergeDoesntFit() throws Exception {
        PrecomputedTable pt = new PrecomputedTable(new Query(), "precomp1");

        Set results = QueryOptimiser.merge(pt, singleTableQuery);
        assertNull(results);

    }
    */
    public void testRemapAliases() throws Exception {
        AbstractTable t1 = new Table("table1", "alias1");
        AbstractTable t2 = new Table("table2", "alias2");
        AbstractTable t3 = new Table("table3", "alias3");
        AbstractTable t4 = new Table("table1", "alias4");
        AbstractTable t5 = new Table("table2", "alias5");
        AbstractTable t6 = new Table("table3", "alias6");

        Map map = new HashMap();
        map.put(t1, t4);
        map.put(t2, t5);
        map.put(t3, t6);

        QueryOptimiser.remapAliases(map);

        assertEquals(t1, map.get(t1));
        assertEquals(t2, map.get(t2));
        assertEquals(t3, map.get(t3));

    }

    public void testCompareConstraints() throws Exception {
        assertTrue(QueryOptimiser.compareConstraints(twoSameTableQuery.getWhere(),
                                                     twoSameTableQuery.getWhere()));
        assertTrue(QueryOptimiser.compareConstraints(singleTableQueryNoConstraints.getWhere(),
                                                     singleTableQuery.getWhere()));
        assertTrue(QueryOptimiser.compareConstraints(singleTableQueryWithTableAlias.getWhere(),
                                                     twoSameTableQuery.getWhere()));

        assertFalse(QueryOptimiser.compareConstraints(singleTableQuery.getWhere(),
                                                      singleTableQueryNoConstraints.getWhere()));
        assertFalse(QueryOptimiser.compareConstraints(twoSameTableQuery.getWhere(),
                                                      singleTableQueryWithTableAlias.getWhere()));
    }


}
