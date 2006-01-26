package org.intermine.model.logmodel;

public class InterMineLoggable implements org.intermine.model.InterMineObject
{
    // Attr: org.intermine.model.logmodel.InterMineLoggable.caller
    protected java.lang.String caller;
    public java.lang.String getCaller() { return caller; }
    public void setCaller(java.lang.String caller) { this.caller = caller; }

    // Attr: org.intermine.model.logmodel.InterMineLoggable.message
    protected java.lang.String message;
    public java.lang.String getMessage() { return message; }
    public void setMessage(java.lang.String message) { this.message = message; }

    // Attr: org.intermine.model.logmodel.InterMineLoggable.timestamp
    protected java.lang.Long timestamp;
    public java.lang.Long getTimestamp() { return timestamp; }
    public void setTimestamp(java.lang.Long timestamp) { this.timestamp = timestamp; }

    // Attr: org.intermine.model.InterMineObject.id
    protected java.lang.Integer id;
    public java.lang.Integer getId() { return id; }
    public void setId(java.lang.Integer id) { this.id = id; }

    public boolean equals(Object o) { return (o instanceof InterMineLoggable && id != null) ? id.equals(((InterMineLoggable)o).getId()) : false; }
    public int hashCode() { return (id != null) ? id.hashCode() : super.hashCode(); }
    public String toString() { return "InterMineLoggable [Caller=\"" + caller + "\", Id=\"" + id + "\", Message=\"" + message + "\", Timestamp=\"" + timestamp + "\"]"; }
}
