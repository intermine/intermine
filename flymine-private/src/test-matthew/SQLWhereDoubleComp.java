
public class SQLWhereDoubleComp extends SQLWhereComparison
{
    private SQLQueryField left, right;
    private int op;

    public SQLWhereDoubleComp (SQLQueryField aLeft, int aOp, SQLQueryField aRight)
    {
        left = aLeft;
        op = aOp;
        right = aRight;
    }

    public String toString()
    {
        return left.getCanonicalName()+OPS[op]+right.getCanonicalName();
    }
}
