import java.util.*;

public class SQLDatabase
{
    private static Map tables = new HashMap();

    public static void addTable(SQLTable a)
    {
        tables.put(a.getTableName(), a);
    }

    public static void createPossibleLink(String aLeftTableName, String aLeftFieldName, String aRightTableName, String aRightFieldName)
    {
        SQLTable leftTable = (SQLTable) tables.get(aLeftTableName);
        SQLTable rightTable = (SQLTable) tables.get(aRightTableName);
        SQLField leftField = leftTable.getField(aLeftFieldName);
        SQLField rightField = rightTable.getField(aRightFieldName);
        PossibleLink link = new PossibleLink(leftField, rightField);
        leftField.addPossibleLink(link);
        rightField.addPossibleLink(link);
    }

    public static String print()
    {
        String retVal = "All tables:\n\n";
        Iterator tableIter = tables.entrySet().iterator();
        while (tableIter.hasNext())
        {
            Map.Entry tableEntry = (Map.Entry) tableIter.next();
            SQLTable table = (SQLTable) tableEntry.getValue();
            retVal += table.toString();
        }
        return retVal;
    }
}

