<%@ tag body-content="empty" %>
<%@ attribute name="str" required="true" %>
<%@ attribute name="delimiter" required="true" %>
<%@ attribute name="outVar" required="true" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%-- return the prefix of a string.  eg. for "foo.bar.thing" and delimiter --
  -- ".", return "foo.bar" --%>

<%
   String str = (String) jspContext.getAttribute("str");
   String delimiter = (String) jspContext.getAttribute("delimiter");
   String outVar = (String) jspContext.getAttribute("outVar");
   int index = str.lastIndexOf(delimiter);
   if (index == -1) {
      request.setAttribute(outVar, str);
   } else {
      request.setAttribute(outVar, str.substring(0, index));
   }
%>
