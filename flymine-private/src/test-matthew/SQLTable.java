import java.util.*;

public class SQLTable
{
    private String tableName;
    private Map fields;

    public SQLTable (String aTableName)
    {
        tableName = aTableName;
        fields = new HashMap();
    }

    public boolean equals(Object a)
    {
        boolean retVal = false;
        if (a instanceof SQLTable)
        {
            retVal = (((SQLTable) a).getTableName() == tableName);
        }
        return retVal;
    }

    public int hashCode()
    {
        return tableName.hashCode();
    }

    public String getTableName()
    {
        return tableName;
    }

    public void addField(String aFieldName)
    {
        SQLField field = new SQLField(aFieldName, this);
        fields.put(field.getFieldName(), field);
    }

    public SQLField getField(String aFieldName)
    {
        return (SQLField) fields.get(aFieldName);
    }

    public String toString()
    {
        String retVal = "Table: "+tableName+"\n";
        Iterator fieldIter = fields.entrySet().iterator();
        while (fieldIter.hasNext())
        {
            Map.Entry fieldEntry = (Map.Entry) fieldIter.next();
            SQLField field = (SQLField) fieldEntry.getValue();
            retVal += "    "+field.toString()+"\n";
        }
        return retVal;
    }
}
