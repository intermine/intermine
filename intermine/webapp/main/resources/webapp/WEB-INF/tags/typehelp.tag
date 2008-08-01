<%@ tag body-content="empty" %>
<%@ attribute name="type" required="true" %>
<%@ attribute name="fullPath" required="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<%-- wraps helplink to properly format the type context help --%>

<%
   String type = (String) jspContext.getAttribute("type");
   String fullPath = (String) jspContext.getAttribute("fullPath");
   if (fullPath == null) {
       request.setAttribute("field", type.substring(type.lastIndexOf(".") + 1));
   } else {
      request.setAttribute("field", type);
   }
   java.util.Map classDescriptions = (java.util.Map) application.getAttribute("classDescriptions");
   String helpText = (String) classDescriptions.get(type);
   request.setAttribute("helpText", helpText);
%>

<c:if test="${!empty helpText}"><im:helplink text="${field}: ${helpText}"/></c:if>
