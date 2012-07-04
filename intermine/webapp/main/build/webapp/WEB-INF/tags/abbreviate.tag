<%@ tag body-content="empty" %>
<%@ attribute name="value" required="true" type="java.lang.String" %>
<%@ attribute name="var" required="false" %>
<%@ attribute name="length" required="true" type="java.lang.Integer" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%  String abbreviated = org.intermine.util.StringUtil.wrapLines(value, length.intValue(), 1).getString(); %>

<c:if test="${!empty var}">
<% String varName = (String) jspContext.getAttribute("var");
   request.setAttribute(varName, jspContext.getAttribute("abbreviated"));%>
</c:if>

<c:if test="${empty var}">
<%= abbreviated %>
</c:if>
