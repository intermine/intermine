
public class SQLQueryTableSubselect extends SQLQueryTable
{
    private SQLQuery subQuery;
    private SQLQuery parent;

    public SQLQueryTableSubselect(SQLQuery aSubQuery, SQLQuery aParent)
    {
        subQuery = aSubQuery;
        parent = aParent;
    }

    public String getCanonicalName()
    {
        return "("+subQuery.createSelect()+") as "+alias;
    }
}
