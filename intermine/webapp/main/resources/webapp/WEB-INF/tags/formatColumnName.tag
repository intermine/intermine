<%@ tag body-content="empty" %>
<%@ attribute name="str" required="true" %>
<%@ attribute name="outVar" required="true" %>

<%-- Formats qualified path. Replaces dots of qualified path with sign '>' --%>
<%
   String str = (String) jspContext.getAttribute("str");
   String outVar = (String) jspContext.getAttribute("outVar");
   // replaces all dots and colons but not dots with following space - they are probably
   // part of name, e.g. 'D. melanogaster' 
   String newStr = str.replaceAll("[:.](?!\\s)", "&nbsp;> ");
   request.setAttribute(outVar, newStr);
%>
