<%@ tag body-content="empty" %>
<%@ attribute name="str" required="true" %>
<%@ attribute name="outVar" required="true" %>

<%-- Formats qualified path. Replaces dots of qualified path with sign '>' --%>
<%
   String str = (String) jspContext.getAttribute("str");
   String outVar = (String) jspContext.getAttribute("outVar");
   // replaces all dots but not dots with preceding space - they are probably part of name, e.g. 'D. melanogaster' 
   String repl = "XXXXXXXXXXXXXXXXXXXXXXXX";
   String newStr = str.replaceAll("[.][ ]", repl);
   newStr = newStr.replaceAll("[.]", "&nbsp;> ");
   newStr = newStr.replaceAll(repl, ". ");
   request.setAttribute(outVar, newStr);
%>
