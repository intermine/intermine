
public class Test1
{
    public static void main(String args[])
    {
        SQLTable tableOne = new SQLTable("flibble");
        SQLDatabase.addTable(tableOne);
        tableOne.addField("field1");
        tableOne.addField("field2");
        SQLTable tableTwo = new SQLTable("banana");
        SQLDatabase.addTable(tableTwo);
        tableTwo.addField("field3");
        tableTwo.addField("field4");
        SQLDatabase.createPossibleLink("flibble", "field1", "banana", "field3");
        System.out.println(SQLDatabase.print());
    }
}
