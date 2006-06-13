<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%-- default page title --%>
<fmt:message key="${pageName}.title" var="pageTitle"/>
<c:out value="${WEB_PROPERTIES['project.title']}: ${pageTitle}" escapeXml="false"/>
