<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- organism.jsp -->
<fmt:setBundle basename="model"/>

<fmt:message key="organism.ncbitaxonomybrowser"/>:
<html:link href="${WEB_PROPERTIES['ncbi.taxonomy.url.prefix']}${object.taxonId}">
  <c:out value="${object.taxonId}"/>
</html:link>
<br/>
<!-- /organism.jsp -->
