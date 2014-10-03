<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!-- dataSourceShort.jsp -->
<html:xhtml/>
<c:set var="interMineObject" value="${object}"/>

<c:if test="${fn:endsWith(object.class.name, '.ReportObject')}">
  <c:set var="interMineObject" value="${object.object}"/>
</c:if>

<html:link href="${interMineObject.url}">
<c:choose>
<c:when test="${fn:startsWith(interMineObject.url, 'http://www.modencode.org')}">
<html:img src="images/extlink.gif"/>
</c:when>
<c:otherwise>
  <html:img src="model/images/${interMineObject.name}_logo_small.png"/>
</c:otherwise>
</c:choose>
</html:link>


<!-- /dataSourceShort.jsp -->
