import java.util.*;

public class SQLQueryTableReal extends SQLQueryTable
{
    private SQLTable realTable;

    public SQLQueryTableReal (SQLTable aRealTable)
    {
        realTable = aRealTable;
    }

    public SQLTable getRealTable()
    {
        return realTable;
    }

    public String getCanonicalName()
    {
        return realTable.getTableName()+" as "+alias;
    }
}
