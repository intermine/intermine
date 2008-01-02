
public class SQLWhereSingleComp extends SQLWhereComparison
{
    private SQLQueryField left;
    private int op;
    private Object right;

    public SQLWhereSingleComp (SQLQueryField aLeft, int aOp, Object aRight)
    {
        left = aLeft;
        op = aOp;
        right = aRight;
    }

    public String toString()
    {
        return left.getCanonicalName()+OPS[op]+right.toString();
    }
}
