<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!-- dataSourceShort.jsp -->
<html:xhtml/>
<c:set var="interMineObject" value="${object}"/>

<c:if test="${fn:endsWith(object.class.name, '.DisplayObject')}">
  <c:set var="interMineObject" value="${object.object}"/>
</c:if>

<html:link href="${interMineObject.url}">
  <html:img src="model/images/${interMineObject.name}_logo_small.png"/>
</html:link>
<!-- /dataSourceShort.jsp -->
