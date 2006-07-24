<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- organismLong.jsp -->
<fmt:setBundle basename="model"/>

<fmt:message key="organism.ncbitaxonomybrowser"/>:
<html:link href="${WEB_PROPERTIES['ncbi.taxonomy.url.prefix']}${object.taxonId}">
  ${object.taxonId}
</html:link>
<br/>
<!-- /organismLong.jsp -->
