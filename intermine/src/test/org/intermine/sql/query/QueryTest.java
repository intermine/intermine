package org.flymine.sql.query;

import junit.framework.*;

public class QueryTest extends TestCase
{
    private Query q1, q2, q3;

    public QueryTest(String arg1) {
        super(arg1);
    }

    public void setUp()
    {
        q1 = new Query();
        Table t = new Table("mytable");
        Constant c = new Constant("1");
        Field f = new Field("a", t);
        SelectValue sv = new SelectValue(f, null);
        q1.addFrom(t);
        q1.addSelect(sv);
        q1.addWhere(new Constraint(f, Constraint.EQ, c));

        q2 = new Query();
        t = new Table("mytable");
        c = new Constant("1");
        f = new Field("a", t);
        sv = new SelectValue(f, null);
        q2.addFrom(t);
        q2.addSelect(sv);
        q2.addWhere(new Constraint(f, Constraint.EQ, c));

        q3 = new Query();
        t = new Table("anotherTable");
        c = new Constant("2");
        f = new Field("b", t);
        sv = new SelectValue(f, null);
        q3.addFrom(t);
        q3.addSelect(sv);
        q3.addWhere(new Constraint(f, Constraint.LT, c));
    }
    public void testGetSQLString() throws Exception {
        Query q = new Query();
        Table t = new Table("mytable");
        Constant c = new Constant("1");
        Field f = new Field("a", t);
        SelectValue sv = new SelectValue(f, null);
        q.addFrom(t);
        q.addSelect(sv);
        q.addWhere(new Constraint(f, Constraint.EQ, c));
        assertEquals("SELECT mytable.a FROM mytable WHERE mytable.a = 1", q.getSQLString());
    }

    public void testEquals() throws Exception {
        assertEquals(q1, q1);
        assertEquals(q1, q2);
        assertTrue("Expected q1 to not equal q3", !q1.equals(q3));
    }

    public void testHashCode() throws Exception {
        assertEquals(q1.hashCode(), q1.hashCode());
        assertEquals(q1.hashCode(), q2.hashCode());
        assertTrue("Expected q1 hashcode not to equal q3 hashcode", q1.hashCode() != q3.hashCode());
    }

    public void testConstructNullString() throws Exception {
        try {
            Query q1 = new Query((String) null);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testConstructEmptyString() throws Exception {
        try {
            Query q1 = new Query("");
            fail("Expected: IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
        }
    }

    public void testConstructIllegalString() throws Exception {
        try {
            Query q1 = new Query("A load of rubbish");
            fail("Expected: IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
        }
    }

    public void testSelectNoAlias() throws Exception {
        Query q1 = new Query("select table1.field1 from table1");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        assertEquals(q2, q1);
    }

    public void testSelectTableAlias() throws Exception {
        Query q1 = new Query("select t1.field1 from table1 as t1");
        Query q1_alt = new Query("select t1.field1 from table1 t1");
        Query q2 = new Query();
        Table t1 = new Table("table1", "t1");
        Field f1 = new Field("field1", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        assertEquals(q2, q1);
        assertEquals(q2, q1_alt);
    }

    public void testSelectFieldAlias() throws Exception {
        Query q1 = new Query("select table1.field1 as alias1 from table1");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        SelectValue sv1 = new SelectValue(f1, "alias1");
        q2.addSelect(sv1);
        q2.addFrom(t1);
        assertEquals(q2, q1);
    }

    public void testSelectFunctionAlias() throws Exception {
        Query q1 = new Query("select max(table1.field1) as alias1 from table1");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Function func1 = new Function(Function.MAX);
        func1.add(f1);
        SelectValue sv1 = new SelectValue(func1, "alias1");
        q2.addSelect(sv1);
        q2.addFrom(t1);
        assertEquals(q2, q1);
    }

    public void testSelectPlusFunctionAlias() throws Exception {
        Query q1 = new Query("select table1.field1 + table1.field2 as alias1 from table1");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t1);
        Function func1 = new Function(Function.PLUS);
        func1.add(f1);
        func1.add(f2);
        SelectValue sv1 = new SelectValue(func1, "alias1");
        q2.addSelect(sv1);
        q2.addFrom(t1);
        assertEquals(q2, q1);
    }

    public void testSelectTwoSameTables() throws Exception {
        Query q1 = new Query("select t1.field1 from table1 t1, table1 t2");
        Query q2 = new Query();
        Table t1 = new Table("table1", "t1");
        Table t2 = new Table("table1", "t2");
        Field f1 = new Field("field1", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addFrom(t2);
        assertEquals(q2, q1);
    }

    public void testSelectTwoDifferentTables() throws Exception {
        Query q1 = new Query("select table1.field1 from table1, table2");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Table t2 = new Table("table2");
        Field f1 = new Field("field1", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addFrom(t2);
        assertEquals(q2, q1);
    }

    public void testSelectTwoDifferentTablesWithAliases() throws Exception {
        Query q1 = new Query("select t1.field1 from table1 t1, table2 t2");
        Query q2 = new Query();
        Table t1 = new Table("table1", "t1");
        Table t2 = new Table("table2", "t2");
        Field f1 = new Field("field1", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addFrom(t2);
        assertEquals(q2, q1);
    }

    public void testSelectFromSubQuery() throws Exception {
        Query q1 = new Query("select t1.field1 from (select table2.field2 from table2) as t1");
        Query q2 = new Query();
        SubQuery sq1 = new SubQuery(new Query("select table2.field2 from table2"), "t1");
        Field f1 = new Field("field1", sq1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(sq1);
        //throw new Exception(q1.getSQLString() + "     " + q2.getSQLString());
        assertEquals(q2, q1);
    }

    public void testWhereOneEqualConstraint() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 where table1.field1 = 1");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Constant c = new Constant("1");
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addWhere(new Constraint(f1, Constraint.EQ, c));
        assertEquals(q2, q1);
    }

    public void testWhereTwoEqualConstraints() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 where table1.field1 = 1 and table1.field2 = 2");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t1);
        Constant c1 = new Constant("1");
        Constant c2 = new Constant("2");
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addWhere(new Constraint(f1, Constraint.EQ, c1));
        q2.addWhere(new Constraint(f2, Constraint.EQ, c2));
        assertEquals(q2, q1);
    }

    public void testWhereFieldLessThanField() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 where table1.field1 < table1.field2");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addWhere(new Constraint(f1, Constraint.LT, f2));
        assertEquals(q2, q1);
    }


    public void testWhereNottedFieldLessThanField() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 where not table1.field1 < table1.field2");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addWhere(new NotConstraint(new Constraint(f1, Constraint.LT, f2)));
        assertEquals(q2, q1);
    }


    public void testWhereFieldLessThanFunction() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 where table1.field1 < avg(table1.field2)");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t1);
        Function func1 = new Function(Function.AVG);
        func1.add(f2);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addWhere(new Constraint(f1, Constraint.LT, func1));
        assertEquals(q2, q1);
    }


    public void testWhereFieldLessThanPlusFunction() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 where table1.field1 < table1.field2 + table1.field3");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t1);
        Field f3 = new Field("field3", t1);
        Function func1 = new Function(Function.PLUS);
        func1.add(f2);
        func1.add(f3);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addWhere(new Constraint(f1, Constraint.LT, func1));
        assertEquals(q2, q1);
    }


    public void testWhereFieldInSubQuery() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 where table1.field1 in (select table2.field2 from table2)");
        Query q2 = new Query();
        Query q3 = new Query();
        Table t1 = new Table("table1");
        Table t2 = new Table("table2");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t2);
        SelectValue sv1 = new SelectValue(f1, null);
        SelectValue sv2 = new SelectValue(f2, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q3.addSelect(sv2);
        q3.addFrom(t2);
        q2.addWhere(new SubQueryConstraint(f1, q3));
        assertEquals(q2, q1);
    }

     public void testWhereConstraintSet() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 where (table1.field1 = table1.field2 or table1.field1 = table1.field3)");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t1);
        Field f3 = new Field("field3", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        ConstraintSet cs1 = new ConstraintSet();
        cs1.add(new Constraint(f1, Constraint.EQ, f2));
        cs1.add(new Constraint(f1, Constraint.EQ, f3));
        q2.addWhere(cs1);
        assertEquals(q2, q1);
    }

     public void testWhereNottedConstraintSet() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 where not (table1.field1 = table1.field2 or table1.field1 = table1.field3)");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t1);
        Field f3 = new Field("field3", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addWhere(new NotConstraint(new Constraint(f1, Constraint.EQ, f2)));
        q2.addWhere(new NotConstraint(new Constraint(f1, Constraint.EQ, f3)));
        assertEquals(q2, q1);
    }

    public void testOrderBySingleField() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 order by table1.field1");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addOrderBy(f1);
        assertEquals(q2, q1);
    }

    public void testOrderByTwoFields() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 order by table1.field1, table1.field2");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addOrderBy(f1);
        q2.addOrderBy(f2);
        assertEquals(q2, q1);
    }

    public void testOrderByFunction() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 order by table1.field1 + table1.field2");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t1);
        Function func1 = new Function(Function.PLUS);
        func1.add(f1);
        func1.add(f2);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addOrderBy(func1);
        assertEquals(q2, q1);
    }

    public void testGroupBySingleField() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 group by table1.field1");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addGroupBy(f1);
        assertEquals(q2, q1);
    }

    public void testGroupByTwoFields() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 group by table1.field1, table1.field2");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addGroupBy(f1);
        q2.addGroupBy(f2);
        assertEquals(q2, q1);
    }

    public void testGroupByFunction() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 group by table1.field1 + table1.field2");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t1);
        Function func1 = new Function(Function.PLUS);
        func1.add(f1);
        func1.add(f2);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addGroupBy(func1);
        assertEquals(q2, q1);
    }

    public void testHavingOneLessThanConstraint() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 group by table1.field1 having table1.field1 < 1");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Constant c = new Constant("1");
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addGroupBy(f1);
        q2.addHaving(new Constraint(f1, Constraint.LT, c));
        assertEquals(q2, q1);
    }

    public void testHavingTwoEqualConstraints() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 group by table1.field1 having table1.field1 = 1 and table1.field2 = 2");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t1);
        Constant c1 = new Constant("1");
        Constant c2 = new Constant("2");
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addGroupBy(f1);
        q2.addHaving(new Constraint(f1, Constraint.EQ, c1));
        q2.addHaving(new Constraint(f2, Constraint.EQ, c2));
        assertEquals(q2, q1);
    }

    public void testHavingFieldLessThanField() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 group by table1.field1 having table1.field1 < table1.field2");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addGroupBy(f1);
        q2.addHaving(new Constraint(f1, Constraint.LT, f2));
        assertEquals(q2, q1);
    }


    public void testHavingNottedFieldLessThanField() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 group by table1.field1 having not table1.field1 < table1.field2");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addGroupBy(f1);
        q2.addHaving(new NotConstraint(new Constraint(f1, Constraint.LT, f2)));
        assertEquals(q2, q1);
    }


    public void testHavingFieldLessThanFunction() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 group by table1.field1 having table1.field1 < avg(table1.field2)");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t1);
        Function func1 = new Function(Function.AVG);
        func1.add(f2);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addGroupBy(f1);
        q2.addHaving(new Constraint(f1, Constraint.LT, func1));
        assertEquals(q2, q1);
    }


    public void testHavingFieldLessThanPlusFunction() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 group by table1.field1 having table1.field1 < table1.field2 + table1.field3");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t1);
        Field f3 = new Field("field3", t1);
        Function func1 = new Function(Function.PLUS);
        func1.add(f2);
        func1.add(f3);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addGroupBy(f1);
        q2.addHaving(new Constraint(f1, Constraint.LT, func1));
        assertEquals(q2, q1);
    }


    public void testHavingFieldInSubQuery() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 group by table1.field1 having table1.field1 in (select table2.field2 from table2)");
        Query q2 = new Query();
        Query q3 = new Query();
        Table t1 = new Table("table1");
        Table t2 = new Table("table2");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t2);
        SelectValue sv1 = new SelectValue(f1, null);
        SelectValue sv2 = new SelectValue(f2, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addGroupBy(f1);
        q3.addSelect(sv2);
        q3.addFrom(t2);
        q2.addHaving(new SubQueryConstraint(f1, q3));
        assertEquals(q2, q1);
    }

     public void testHavingConstraintSet() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 group by table1.field1 having (table1.field1 = table1.field2 or table1.field1 = table1.field3)");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t1);
        Field f3 = new Field("field3", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addGroupBy(f1);
        ConstraintSet cs1 = new ConstraintSet();
        cs1.add(new Constraint(f1, Constraint.EQ, f2));
        cs1.add(new Constraint(f1, Constraint.EQ, f3));
        q2.addHaving(cs1);
        assertEquals(q2, q1);
    }

     public void testHavingNottedConstraintSet() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 group by table1.field1 having not (table1.field1 = table1.field2 or table1.field1 = table1.field3)");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t1);
        Field f3 = new Field("field3", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addGroupBy(f1);
        q2.addHaving(new NotConstraint(new Constraint(f1, Constraint.EQ, f2)));
        q2.addHaving(new NotConstraint(new Constraint(f1, Constraint.EQ, f3)));
        assertEquals(q2, q1);
    }

    public void testSelectDistinct() throws Exception {
        Query q1 = new Query("select distinct table1.field1 from table1");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.setDistinct(true);
        assertEquals(q2, q1);
    }

    public void testExplain() throws Exception {
        Query q1 = new Query("explain select table1.field1 from table1");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.setExplain(true);
        assertEquals(q2, q1);
    }

    public void testQuiteComplexQuery() throws Exception {
        Query q1 = new Query("select t1.field1 as first, table2.field1, count(*) as c, max(t1.field2) as mx from table1 t1, table2, (select table3.field2 as f1 from table3) as t3 where t1.field3 = table2.field1 and t3.f1 = table2.field3 and (t1.field4 = table2.field2 or t1.field4 = table2.field3) group by t1.field1, table2.field1 having (t1.field1 = table2.field2 or t1.field1 = table2.field3) order by t1.field1, table2.field1 limit 100 offset 10");
        Query q2 = new Query();
        Table t1 = new Table("table1", "t1");
        Table t2 = new Table("table2");
        Table t3 = new Table("table3");
        Field t1f1 = new Field("field1", t1);
        Field t1f2 = new Field("field2", t1);
        Field t1f3 = new Field("field3", t1);
        Field t1f4 = new Field("field4", t1);
        Field t2f1 = new Field("field1", t2);
        Field t2f2 = new Field("field2", t2);
        Field t2f3 = new Field("field3", t2);
        Field t3f2 = new Field("field2", t3);

        Query q3 = new Query();

        q3.addSelect(new SelectValue(t3f2, "f1"));
        q3.addFrom(t3);

        SubQuery sq1 = new SubQuery(q3, "t3");
        Field t3f1 = new Field("f1", sq1);

        SelectValue sv1 = new SelectValue(t1f1, "first");
        SelectValue sv2 = new SelectValue(t2f1, null);
        SelectValue sv3 = new SelectValue(new Function(Function.COUNT), "c");
        Function func1 = new Function(Function.MAX);
        func1.add(t1f2);
        SelectValue sv4 = new SelectValue(func1, "mx");

        ConstraintSet cs1 = new ConstraintSet();
        cs1.add(new Constraint(t1f4, Constraint.EQ, t2f2));
        cs1.add(new Constraint(t1f4, Constraint.EQ, t2f3));

        ConstraintSet cs2 = new ConstraintSet();
        cs2.add(new Constraint(t1f1, Constraint.EQ, t2f2));
        cs2.add(new Constraint(t1f1, Constraint.EQ, t2f3));


        q2.addSelect(sv1);
        q2.addSelect(sv2);
        q2.addSelect(sv3);
        q2.addSelect(sv4);
        q2.addFrom(t1);
        q2.addFrom(t2);
        q2.addFrom(sq1);

        q2.addWhere(new Constraint(t1f3, Constraint.EQ, t2f1));
        q2.addWhere(new Constraint(t2f3, Constraint.EQ, t3f1));
        q2.addWhere(cs1);

        q2.addGroupBy(t1f1);
        q2.addGroupBy(t2f1);

        q2.addHaving(cs2);

        q2.addOrderBy(t1f1);
        q2.addOrderBy(t2f1);

        q2.setLimitOffset(100,10);

        assertEquals(q2, q1);
    }

    public void testLimitOffset() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 limit 100 offset 10");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.setLimitOffset(100,10);
        assertEquals(q2, q1);
    }

}
