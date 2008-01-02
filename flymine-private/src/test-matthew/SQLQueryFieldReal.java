import java.util.*;

public class SQLQueryFieldReal extends SQLQueryField
{
    private String fieldName;
    private SQLQueryTable parentTable;

    public SQLQueryFieldReal (String aFieldName, SQLQueryTable aParentTable)
    {
        fieldName = aFieldName;
        parentTable = aParentTable;
    }

    public boolean equals(Object a)
    {
        boolean retVal = false;
        if (a instanceof SQLQueryFieldReal)
        {
            SQLQueryFieldReal b = (SQLQueryFieldReal) a;
            retVal = (b.getFieldName() == fieldName) && (b.getParentTable().equals(parentTable));
        }
        return retVal;
    }

    public int hashCode()
    {
        return fieldName.hashCode() + parentTable.hashCode();
    }

    public String getFieldName()
    {
        return fieldName;
    }

    public SQLQueryTable getParentTable()
    {
        return parentTable;
    }

    public String getCanonicalName()
    {
        return parentTable.getAlias()+"."+fieldName;
    }
}
