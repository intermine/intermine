<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>

<tiles:importAttribute/>

<!-- description.jsp -->
<c:if test="${!empty pageName}">
  <fmt:message key="${pageName}.description" var="description"/>
  <c:if test="${!empty description}">
    <div class="description">
      <c:out value="${description}" escapeXml="false"/>
      <fmt:message key="${pageName}.help" var="help"/>
      <c:if test="${!empty help}">
        [<html:link href="/webapp/${pageName}.html"><c:out value="${help}"/></html:link>]
      </c:if>
      <br/><br/>
    </div>
  </c:if>
</c:if>
<!-- /description.jsp -->
