<%@ tag body-content="empty" %>
<%@ attribute name="text" required="false" %>
<%@ attribute name="key" required="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<%-- outputs a superscript question mark which, when clicked shows some context help --%>

<c:if test="${!empty key}">
  <fmt:message var="text" key="${key}"/>
</c:if>

<c:if test="${!empty text}"><sup><im:help text="${text}">?</im:help></sup></c:if>