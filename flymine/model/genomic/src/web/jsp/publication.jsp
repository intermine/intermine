<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<fmt:setBundle basename="model"/>

<!-- publication.jsp -->
<fmt:message key="publication.pubmed"/>:
<html:link href="${WEB_PROPERTIES['pubmed.url.prefix']}${object.pubMedId}">
  ${object.pubMedId}
</html:link>
<!-- /publication.jsp -->
