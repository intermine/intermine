<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<tiles:importAttribute/>

<!-- description.jsp -->
<c:if test="${!empty pageName}">
  <fmt:message key="${pageName}.description" var="description"/>
  <c:if test="${!empty description}">
    <tr>
    <th class="title" align="left">
      <div class="description">
        <c:out value="${description}" escapeXml="false"/>
        <fmt:message key="${pageName}.help" var="help"/>
      </div>
    </th>
    <th class="help" align="right">
      <c:if test="${!empty help}">
        [<html:link href="${WEB_PROPERTIES['project.sitePrefix']}/doc/webapp/${pageName}.html"><c:out value="${help}"/></html:link>]
      </c:if>
    </th>
    </tr>
  </c:if>
</c:if>
<!-- /description.jsp -->
