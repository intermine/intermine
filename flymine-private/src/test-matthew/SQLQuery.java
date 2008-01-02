import java.util.*;

public class SQLQuery
{
    private Map tables;
    private SQLWhereClause whereClause;
    private Set showableFields;
    private Set participatingFields;
    private boolean distinct;
    private Set groupBy;

    public SQLQuery ()
    {
        tables = new HashMap();
        whereClause = new SQLWhereClause();
        showableFields = new HashSet();
        participatingFields = new HashSet();
        groupBy = new HashSet();
    }

    public void addTable(SQLQueryTable table)
    {
        tables.put(table.getAlias(), table);
    }

    public void addField(SQLQueryField field)
    {
        showableFields.add(field);
        participatingFields.add(field);
    }

    public void addGroupByField(SQLQueryField field)
    {
        groupBy.add(field);
        participatingFields.add(field);
    }

    public void setDistinct(boolean isDistinct)
    {
        distinct = isDistinct;
    }

    public void addComparison(SQLQueryField left, int op, SQLQueryField right)
    {
        participatingFields.add(left);
        participatingFields.add(right);
        whereClause.addComparison(new SQLWhereDoubleComp(left, op, right));
    }

    public void addComparison(SQLQueryField left, int op, Object right)
    {
        participatingFields.add(left);
        whereClause.addComparison(new SQLWhereSingleComp(left, op, right));
    }

    public Set getParticipatingFields()
    {
        return participatingFields;
    }

    public String createSelect()
    {
        String retVal = (distinct ? "select distinct " : "select ");
        Iterator fieldIter = showableFields.iterator();
        boolean needComma = false;
        while (fieldIter.hasNext())
        {
            SQLQueryField field = (SQLQueryField) fieldIter.next();
            retVal += (needComma ? ", " : "") + field.getCanonicalName();
            needComma = true;
        }
        retVal += " from ";
        Iterator tableIter = tables.entrySet().iterator();
        needComma = false;
        while (tableIter.hasNext())
        {
            Map.Entry tableEntry = (Map.Entry) tableIter.next();
            SQLQueryTable table = (SQLQueryTable) tableEntry.getValue();
            retVal += (needComma ? ", " : "") + table.getCanonicalName();
            needComma = true;
        }
        retVal += whereClause.createClause();
        Iterator groupIter = groupBy.iterator();
        needComma = false;
        while (groupIter.hasNext())
        {
            SQLQueryField group = (SQLQueryField) groupIter.next();
            retVal += (needComma ? ", " : " group by ") + group.getCanonicalName();
            needComma = true;
        }
        return retVal;
    }
}
