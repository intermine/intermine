<%@ tag body-content="empty" %>
<%@ attribute name="type" required="true" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<%-- wraps helplink to properly format the type context help --%>

<c:if test="${!empty classDescriptions[type]}"><im:helplink text="${type}: ${classDescriptions[type]}"/></c:if>