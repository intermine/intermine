
public class Test3
{
    public static void main(String args[])
    {
        SQLTable tableOne = new SQLTable("jointest4_1");
        SQLDatabase.addTable(tableOne);
        tableOne.addField("keya");
        tableOne.addField("keyb");
        SQLTable tableTwo = new SQLTable("jointest4_2");
        SQLDatabase.addTable(tableTwo);
        tableTwo.addField("keyb");
        tableTwo.addField("keyc");
        SQLDatabase.createPossibleLink("jointest4_1", "keyb", "jointest4_2", "keyb");
        System.out.println(SQLDatabase.print());


        SQLQuery query = new SQLQuery();
        query.setDistinct(false);
        SQLQueryTable qTableOne = new SQLQueryTableReal(tableOne);
        query.addTable(qTableOne);
        SQLQueryField fielda = new SQLQueryFieldReal("keya", qTableOne);
        SQLQueryField fieldb = new SQLQueryFieldReal("keyb", qTableOne);
        query.addField(fieldb);
        query.addField(new SQLQueryFieldCount());
        query.addGroupByField(fieldb);
        System.out.println(query.createSelect());

        SQLQuery queryb = new SQLQuery();
        SQLQueryTable qTableTwo = new SQLQueryTableReal(tableTwo);
        SQLQueryTable qTableThree = new SQLQueryTableSubselect(query, queryb);
        queryb.addTable(qTableTwo);
        queryb.addTable(qTableThree);
        SQLQueryField fieldc = new SQLQueryFieldReal("keyb", qTableTwo);
        SQLQueryField fieldd = new SQLQueryFieldReal("keyc", qTableTwo);
        SQLQueryField fielde = new SQLQueryFieldReal("keyb", qTableThree);
        SQLQueryField fieldf = new SQLQueryFieldReal("count", qTableThree);
        queryb.addField(fieldc);
        queryb.addField(fieldd);
        queryb.addField(fieldf);
        queryb.addComparison(fielde, SQLWhereComparison.OP_EQ, fieldc);
        queryb.addComparison(fieldd, SQLWhereComparison.OP_LT, new Integer(100000));
        System.out.println(queryb.createSelect());
    }
}

