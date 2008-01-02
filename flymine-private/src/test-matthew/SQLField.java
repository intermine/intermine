import java.util.*;

public class SQLField
{
    private String fieldName;
    private SQLTable parentTable;
    private Set possibleLinks;

    public SQLField (String aFieldName, SQLTable aParentTable)
    {
        fieldName = aFieldName;
        parentTable = aParentTable;
        possibleLinks = new HashSet();
    }

    public boolean equals(Object a)
    {
        boolean retVal = false;
        if (a instanceof SQLField)
        {
            SQLField b = (SQLField) a;
            retVal = ((b.getFieldName() == fieldName) && (b.getParentTable().equals(parentTable)));
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

    public SQLTable getParentTable()
    {
        return parentTable;
    }

    public void addPossibleLink(PossibleLink a)
    {
        possibleLinks.add(a);
    }

    public String toString()
    {
        String retVal = fieldName;
        Iterator linkIter = possibleLinks.iterator();
        while (linkIter.hasNext())
        {
            PossibleLink link = (PossibleLink) linkIter.next();
            SQLField linkedField = link.getOtherField(this);
            retVal += "\n        Possible Link -> "+linkedField.getParentTable().getTableName()+"."+linkedField.getFieldName();
        }
        return retVal;
    }
}
