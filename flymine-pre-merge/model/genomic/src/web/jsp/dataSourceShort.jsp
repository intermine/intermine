<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- dataSourceShort.jsp -->
<html:xhtml/>
<c:set var="interMineObject" value="${object}"/>
<c:if test="${object.class.name == 'org.intermine.web.results.DisplayObject'}">
  <c:set var="interMineObject" value="${object.object}"/>
</c:if>

<html:link href="${interMineObject.url}">
  <html:img src="model/${interMineObject.name}_logo_small.png"/>
</html:link>
<!-- /dataSourceShort.jsp -->
