import java.util.*;

public abstract class SQLQueryTable
{
	protected String alias;

	public SQLQueryTable ()
	{
		alias = Utils.uniqueString();
	}
	
	public String getAlias()
	{
		return alias;
	}

	public abstract String getCanonicalName();
}
