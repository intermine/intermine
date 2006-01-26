package org.intermine.model.logmodel;

public class LoggableQuery extends org.intermine.model.logmodel.InterMineLoggable
{
    // Attr: org.intermine.model.logmodel.LoggableQuery.initiator
    protected java.lang.String initiator;
    public java.lang.String getInitiator() { return initiator; }
    public void setInitiator(java.lang.String initiator) { this.initiator = initiator; }

    // Attr: org.intermine.model.logmodel.LoggableQuery.queryOQL
    protected java.lang.String queryOQL;
    public java.lang.String getQueryOQL() { return queryOQL; }
    public void setQueryOQL(java.lang.String queryOQL) { this.queryOQL = queryOQL; }

    // Attr: org.intermine.model.logmodel.LoggableQuery.querySQL
    protected java.lang.String querySQL;
    public java.lang.String getQuerySQL() { return querySQL; }
    public void setQuerySQL(java.lang.String querySQL) { this.querySQL = querySQL; }

    // Attr: org.intermine.model.logmodel.LoggableQuery.optimise
    protected java.lang.Long optimise;
    public java.lang.Long getOptimise() { return optimise; }
    public void setOptimise(java.lang.Long optimise) { this.optimise = optimise; }

    // Attr: org.intermine.model.logmodel.LoggableQuery.estimated
    protected java.lang.Long estimated;
    public java.lang.Long getEstimated() { return estimated; }
    public void setEstimated(java.lang.Long estimated) { this.estimated = estimated; }

    // Attr: org.intermine.model.logmodel.LoggableQuery.execute
    protected java.lang.Long execute;
    public java.lang.Long getExecute() { return execute; }
    public void setExecute(java.lang.Long execute) { this.execute = execute; }

    // Attr: org.intermine.model.logmodel.LoggableQuery.acceptable
    protected java.lang.Long acceptable;
    public java.lang.Long getAcceptable() { return acceptable; }
    public void setAcceptable(java.lang.Long acceptable) { this.acceptable = acceptable; }

    // Attr: org.intermine.model.logmodel.LoggableQuery.conversion
    protected java.lang.Long conversion;
    public java.lang.Long getConversion() { return conversion; }
    public void setConversion(java.lang.Long conversion) { this.conversion = conversion; }

    public boolean equals(Object o) { return (o instanceof LoggableQuery && id != null) ? id.equals(((LoggableQuery)o).getId()) : false; }
    public int hashCode() { return (id != null) ? id.hashCode() : super.hashCode(); }
    public String toString() { return "LoggableQuery [Acceptable=\"" + acceptable + "\", Caller=\"" + caller + "\", Conversion=\"" + conversion + "\", Estimated=\"" + estimated + "\", Execute=\"" + execute + "\", Id=\"" + id + "\", Initiator=\"" + initiator + "\", Message=\"" + message + "\", Optimise=\"" + optimise + "\", QueryOQL=\"" + queryOQL + "\", QuerySQL=\"" + querySQL + "\", Timestamp=\"" + timestamp + "\"]"; }
}
