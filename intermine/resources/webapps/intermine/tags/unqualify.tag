<%@ tag body-content="empty" %>
<%@ attribute name="className" required="true" %>
<%@ attribute name="var" required="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<c:set var="varOut" value="${fn:split(className, '.')[fn:length(fn:split(className, '.')) - 1]}"/>
<c:if test="${!empty var}">
<%
    String varName = (String) jspContext.getAttribute("var");
    //jspContext.setAttribute(varName, jspContext.getAttribute("varOut"), PageContext.PAGE_SCOPE);
    request.setAttribute(varName, jspContext.getAttribute("varOut"));
%>
</c:if>
<c:if test="${empty var}">
${varOut}
</c:if>
