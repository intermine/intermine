
public class Test2
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
        SQLDatabase.createPossibleLink("jointest4_1", "keya", "jointest4_2", "keyb");
        System.out.println(SQLDatabase.print());


        SQLQuery query = new SQLQuery();
        query.setDistinct(false);
        SQLQueryTable qTableOne = new SQLQueryTableReal(tableOne);
        SQLQueryTable qTableTwo = new SQLQueryTableReal(tableTwo);
        query.addTable(qTableOne);
        query.addTable(qTableTwo);
        SQLQueryField fielda = new SQLQueryFieldReal("keya", qTableOne);
        SQLQueryField fieldb = new SQLQueryFieldReal("keyb", qTableOne);
        SQLQueryField fieldc = new SQLQueryFieldReal("keyb", qTableTwo);
        SQLQueryField fieldd = new SQLQueryFieldReal("keyc", qTableTwo);
        query.addField(fielda);
        query.addField(fieldb);
        query.addField(fieldc);
        query.addComparison(fieldb, SQLWhereComparison.OP_EQ, fieldc);
        System.out.println(query.createSelect());
    }
}

