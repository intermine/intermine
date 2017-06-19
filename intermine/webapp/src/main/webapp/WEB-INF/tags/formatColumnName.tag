<%@ tag body-content="empty" %>
<%@ attribute name="str" required="true" %>
<%@ attribute name="outVar" required="true" %>

<%-- Formats qualified path. Replaces dots of qualified path with sign '>' --%>
<%
   String str = (String) jspContext.getAttribute("str");
   String outVar = (String) jspContext.getAttribute("outVar");
   request.setAttribute(outVar, org.intermine.web.logic.WebUtil.formatColumnName(str));
%>
