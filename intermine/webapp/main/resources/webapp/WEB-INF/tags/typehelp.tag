<%@ tag body-content="empty" %>
<%@ attribute name="type" required="true" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<%-- wraps helplink to properly format the type context help --%>

<%
   String type = (String) jspContext.getAttribute("type");
   
   request.setAttribute("field", type.substring(type.lastIndexOf(".") + 1));
%>


<c:if test="${!empty classDescriptions[type]}"><im:helplink text="${field}: ${classDescriptions[type]}"/></c:if>
