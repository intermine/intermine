package org.flymine.sql.precompute;

import junit.framework.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
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
        AbstractTable t7 = new Table("table4", "alias1");

        Map map = new HashMap();
        map.put(t1, t4);
        map.put(t2, t5);
        map.put(t3, t6);

        Set tables = new HashSet();
        tables.add(t4);
        tables.add(t5);
        tables.add(t6);
        tables.add(t7);

        QueryOptimiser.remapAliases(map, tables);

        assertEquals(t1, map.get(t1));
        assertEquals(t2, map.get(t2));
        assertEquals(t3, map.get(t3));
        assertTrue(!"alias1".equals(t7.getAlias()));
    }

    public void testCompareConstraints() throws Exception {
        Set equalsSet = new HashSet();
        assertTrue(QueryOptimiser.compareConstraints(twoSameTableQuery.getWhere(),
                                                     twoSameTableQuery.getWhere(), equalsSet));
        assertEquals(twoSameTableQuery.getWhere(), equalsSet);
        equalsSet = new HashSet();
        assertTrue(QueryOptimiser.compareConstraints(singleTableQueryNoConstraints.getWhere(),
                                                     singleTableQuery.getWhere(), equalsSet));
        assertEquals(singleTableQueryNoConstraints.getWhere(), equalsSet);
        equalsSet = new HashSet();
        assertTrue(QueryOptimiser.compareConstraints(singleTableQueryWithTableAlias.getWhere(),
                                                     twoSameTableQuery.getWhere(), equalsSet));
        assertEquals(singleTableQueryWithTableAlias.getWhere(), equalsSet);

        equalsSet = new HashSet();
        assertFalse(QueryOptimiser.compareConstraints(singleTableQuery.getWhere(),
                                                      singleTableQueryNoConstraints.getWhere(),
                                                      equalsSet));
        // equalsSet is now undefined.
        equalsSet = new HashSet();
        assertFalse(QueryOptimiser.compareConstraints(twoSameTableQuery.getWhere(),
                                                      singleTableQueryWithTableAlias.getWhere(),
                                                      equalsSet));
        // equalsSet is now undefined.
    }

    public void testCompareSelectLists() throws Exception {
        SelectValue v1 = new SelectValue(new Constant("hello"), "alias1");
        SelectValue v2 = new SelectValue(new Constant("hello"), "alias2");
        SelectValue v3 = new SelectValue(new Constant("a"), "alias3");
        SelectValue v4 = new SelectValue(new Constant("a"), "alias4");
        SelectValue v5 = new SelectValue(new Constant("a"), "alias5");
        SelectValue v6 = new SelectValue(new Constant("flibble"), "alias6");

        List l1 = new ArrayList();
        l1.add(v1);
        l1.add(v3);
        List l2 = new ArrayList();
        l2.add(v2);
        l2.add(v4);
        l2.add(v5);
        List l3 = new ArrayList();
        l3.add(v2);
        l3.add(v4);
        l3.add(v6);
        List l4 = new ArrayList();

        assertTrue(QueryOptimiser.compareSelectLists(l1, l2));
        assertTrue(QueryOptimiser.compareSelectLists(l2, l1));
        assertTrue(QueryOptimiser.compareSelectLists(l1, l1));
        assertTrue(QueryOptimiser.compareSelectLists(l1, l3));
        assertFalse(QueryOptimiser.compareSelectLists(l3, l1));
        assertTrue(QueryOptimiser.compareSelectLists(l4, l1));
        assertFalse(QueryOptimiser.compareSelectLists(l1, l4));
        assertTrue(QueryOptimiser.compareSelectLists(l4, l4));
    }

    public void testFindTableForAlias() throws Exception {
        Set set = new HashSet();
        Table t1 = new Table("table1", "alias1");
        Table t2 = new Table("table2", "alias2");
        Table t3 = new Table("table2", "alias3");
        set.add(t1);
        set.add(t2);
        set.add(t3);

        assertEquals(t1, QueryOptimiser.findTableForAlias("alias1", set));
        assertEquals(t2, QueryOptimiser.findTableForAlias("alias2", set));
        assertEquals(t3, QueryOptimiser.findTableForAlias("alias3", set));
        assertNull(QueryOptimiser.findTableForAlias("alias4", set));
    }

    public void testCreateValueMap() throws Exception {
        Collection values = new HashSet();
        AbstractValue v1 = new Constant("c1");
        AbstractValue v2 = new Constant("c2");
        AbstractValue v3 = new Constant("c3");
        SelectValue s1 = new SelectValue(v1, "alias1");
        SelectValue s2 = new SelectValue(v2, "alias2");
        SelectValue s3 = new SelectValue(v3, "alias3");
        values.add(s1);
        values.add(s2);
        values.add(s3);

        Map result = new HashMap();
        result.put(v1, s1);
        result.put(v2, s2);
        result.put(v3, s3);

        assertEquals(result, QueryOptimiser.createValueMap(values));
    }

    public void testReconstructAbstractValue() throws Exception {
        Collection values = new HashSet();
        Table t1 = new Table("table1", "tablealias1");
        Table t2 = new Table("table2", "tablealias2");
        AbstractValue v1 = new Constant("c1");
        AbstractValue v2 = new Field("field1", t1);
        Function v3 = new Function(Function.PLUS);
        v3.add(v1);
        v3.add(v2);
        AbstractValue v4 = new Constant("c4");
        Function v6 = new Function(Function.MINUS);
        v6.add(v1);
        v6.add(v2);
        Function v7 = new Function(Function.MAX);
        v7.add(v2);
        AbstractValue v8 = new Function(Function.COUNT);
        AbstractValue v9 = new Field("field2", t1);
        AbstractValue v10 = new Field("field3", t2);
        Function v11 = new Function(Function.MULTIPLY);
        v11.add(v9);
        v11.add(v10);
        SelectValue s1 = new SelectValue(v1, "alias1");
        SelectValue s2 = new SelectValue(v2, "alias2");
        SelectValue s3 = new SelectValue(v3, "alias3");
        Map valueMap = new HashMap();
        valueMap.put(v1, s1);
        valueMap.put(v2, s2);
        valueMap.put(v3, s3);

        Table precomputedSqlTable = new Table("precomp1", "precompalias1");
        Set tableSet = new HashSet();
        tableSet.add(t1);

        assertEquals(new Field("alias1", precomputedSqlTable), QueryOptimiser.reconstructAbstractValue(v1, precomputedSqlTable, valueMap, tableSet, true));
        assertEquals(new Field("alias1", precomputedSqlTable), QueryOptimiser.reconstructAbstractValue(v1, precomputedSqlTable, valueMap, tableSet, false));
        assertEquals(new Field("alias2", precomputedSqlTable), QueryOptimiser.reconstructAbstractValue(v2, precomputedSqlTable, valueMap, tableSet, true));
        assertEquals(new Field("alias2", precomputedSqlTable), QueryOptimiser.reconstructAbstractValue(v2, precomputedSqlTable, valueMap, tableSet, false));
        assertEquals(new Field("alias3", precomputedSqlTable), QueryOptimiser.reconstructAbstractValue(v3, precomputedSqlTable, valueMap, tableSet, true));
        assertEquals(new Field("alias3", precomputedSqlTable), QueryOptimiser.reconstructAbstractValue(v3, precomputedSqlTable, valueMap, tableSet, false));
        assertEquals(v4, QueryOptimiser.reconstructAbstractValue(v4, precomputedSqlTable, valueMap, tableSet, true));
        assertEquals(v4, QueryOptimiser.reconstructAbstractValue(v4, precomputedSqlTable, valueMap, tableSet, false));
        Function ev6 = new Function(Function.MINUS);
        ev6.add(new Field("alias1", precomputedSqlTable));
        ev6.add(new Field("alias2", precomputedSqlTable));
        assertEquals(ev6, QueryOptimiser.reconstructAbstractValue(v6, precomputedSqlTable, valueMap, tableSet, true));
        assertEquals(ev6, QueryOptimiser.reconstructAbstractValue(v6, precomputedSqlTable, valueMap, tableSet, false));
        try {
            QueryOptimiser.reconstructAbstractValue(v7, precomputedSqlTable, valueMap, tableSet, true);
            fail("Expected QueryOptimiserException");
        } catch (QueryOptimiserException e) {
        }
        Function ev7 = new Function(Function.MAX);
        ev7.add(new Field("alias2", precomputedSqlTable));
        assertEquals(ev7, QueryOptimiser.reconstructAbstractValue(v7, precomputedSqlTable, valueMap, tableSet, false));
        try {
            QueryOptimiser.reconstructAbstractValue(v8, precomputedSqlTable, valueMap, tableSet, true);
            fail("Expected QueryOptimiserException");
        } catch (QueryOptimiserException e) {
        }
        AbstractValue ev8 = new Function(Function.COUNT);
        assertEquals(ev8, QueryOptimiser.reconstructAbstractValue(v8, precomputedSqlTable, valueMap, tableSet, false));
        try {
            QueryOptimiser.reconstructAbstractValue(v9, precomputedSqlTable, valueMap, tableSet, true);
            fail("Expected QueryOptimiserException");
        } catch (QueryOptimiserException e) {
        }
        try {
            QueryOptimiser.reconstructAbstractValue(v9, precomputedSqlTable, valueMap, tableSet, false);
            fail("Expected QueryOptimiserException");
        } catch (QueryOptimiserException e) {
        }
        assertEquals(v10, QueryOptimiser.reconstructAbstractValue(v10, precomputedSqlTable, valueMap, tableSet, true));
        assertEquals(v10, QueryOptimiser.reconstructAbstractValue(v10, precomputedSqlTable, valueMap, tableSet, false));
        try {
            QueryOptimiser.reconstructAbstractValue(v11, precomputedSqlTable, valueMap, tableSet, true);
            fail("Expected QueryOptimiserException");
        } catch (QueryOptimiserException e) {
        }
        try {
            QueryOptimiser.reconstructAbstractValue(v11, precomputedSqlTable, valueMap, tableSet, false);
            fail("Expected QueryOptimiserException");
        } catch (QueryOptimiserException e) {
        }
    }

    public void testReconstructSelectValues() throws Exception {
        Collection values = new HashSet();
        AbstractValue v1 = new Constant("c1");
        AbstractValue v2 = new Constant("c2");
        AbstractValue v3 = new Constant("c3");
        AbstractValue v4 = new Constant("c4");
        SelectValue sq1 = new SelectValue(v1, "queryalias1");
        SelectValue sq2 = new SelectValue(v2, "queryalias2");
        SelectValue sq3 = new SelectValue(v3, "queryalias3");
        SelectValue sq4 = new SelectValue(v4, "queryalias4");
        SelectValue sp1 = new SelectValue(v1, "precompalias1");
        SelectValue sp2 = new SelectValue(v2, "precompalias2");
        Map valueMap = new HashMap();
        valueMap.put(v1, sp1);
        valueMap.put(v2, sp2);
        List oldSelect = new ArrayList();
        oldSelect.add(sq1);
        oldSelect.add(sq2);
        oldSelect.add(sq3);
        oldSelect.add(sq4);
        Query newQuery = new Query();
        Table precomputedSqlTable = new Table("precomp1", "precomptablealias1");
        Set tableSet = new HashSet();
        QueryOptimiser.reconstructSelectValues(oldSelect, precomputedSqlTable, valueMap, tableSet, true, newQuery);

        SelectValue se1 = new SelectValue(new Field("precompalias1", precomputedSqlTable), "queryalias1");
        SelectValue se2 = new SelectValue(new Field("precompalias2", precomputedSqlTable), "queryalias2");
        SelectValue se3 = new SelectValue(v3, "queryalias3");
        SelectValue se4 = new SelectValue(v4, "queryalias4");
        List expectedSelect = new ArrayList();
        expectedSelect.add(se1);
        expectedSelect.add(se2);
        expectedSelect.add(se3);
        expectedSelect.add(se4);

        assertEquals(expectedSelect, newQuery.getSelect());
    }
}
