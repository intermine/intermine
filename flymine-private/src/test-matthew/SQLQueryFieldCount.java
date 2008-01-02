import java.util.*;

public class SQLQueryFieldCount extends SQLQueryField
{
    public SQLQueryFieldCount ()
    {
    }

    public boolean equals(Object a)
    {
        return (a instanceof SQLQueryFieldCount);
    }

    public int hashCode()
    {
        return "count(*)".hashCode();
    }

    public SQLQueryTable getParentTable()
    {
        return null;
    }

    public String getCanonicalName()
    {
        return "count(*)";
    }
}
