import java.util.*;

public class SQLWhereClause
{
    private Set comparisons;

    public SQLWhereClause ()
    {
        comparisons = new HashSet();
    }

    public void addComparison(SQLWhereComparison a)
    {
        comparisons.add(a);
    }

    public String createClause()
    {
        String retVal = "";
        Iterator compIter = comparisons.iterator();
        boolean needComma = false;
        while (compIter.hasNext())
        {
            SQLWhereComparison comp = (SQLWhereComparison) compIter.next();
            retVal += (needComma ? " AND " : " where ")+comp.toString();
            needComma = true;
        }
        return retVal;
    }
}
