<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>

<!-- organism.jsp -->

<fmt:setBundle basename="model"/>

<c:out value="${object.name}"/><br/>

<html:link href="${WEB_PROPERTIES['ncbi.taxonomy.url.prefix']}${object.taxonId}">
  <fmt:message key="organism.ncbitaxonomybrowser"/>:

  <c:out value="${object.taxonId}"/>
</html:link>
<br/>
<!-- /organism.jsp -->
