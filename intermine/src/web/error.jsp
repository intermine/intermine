<%@ page import="java.util.*" %>
<%@ page import="java.io.*" %>

<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<html:errors/>
<h2>An error occured with your query</h2>
<logic:present scope="request" name="exception">
    <br/>
    Stack Trace:<br/><code>
    <% 
    Exception exception = (Exception) request.getAttribute("exception");
    CharArrayWriter c=new CharArrayWriter();
    PrintWriter e=new PrintWriter(c);
    exception.printStackTrace(e);
    e.flush(); %>
    <%=c.toString()%>
    </code>
</logic:present>

<html:link page="/buildquery.do"><bean:message key="index.query"/></html:link>
