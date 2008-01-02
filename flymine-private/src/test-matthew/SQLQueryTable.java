import java.util.*;

public abstract class SQLQueryTable
{
    protected String alias;
    protected Set fields;

    public SQLQueryTable ()
    {
        alias = Utils.uniqueString();
        fields = new HashSet();
    }

    public String getAlias()
    {
        return alias;
    }

    public abstract String getCanonicalName();
}
