
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>


<fmt:message key="aspect.title" var="pageTitle">
  <fmt:param value="${param.name}"/>
</fmt:message>
<c:out value="${WEB_PROPERTIES['project.title']}: ${pageTitle}" escapeXml="false"/>

