<%@ tag body-content="empty" %>
<%@ attribute name="type" required="true" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<%-- Formats and decorates browser output for a model class type --%>

<c:if test="${!empty classDescriptions[type]}"><sup><im:help text="${type}: ${classDescriptions[type]}">?</im:help></sup></c:if>