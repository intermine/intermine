<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>

<tiles:importAttribute scope="request"/>

<!-- objectDetailsPage.jsp -->
<c:choose>
  <c:when test="${object != null}">
    <tiles:insert name="objectDetails.tile"/>
    <br/>
  </c:when>
  <c:otherwise>
    null
  </c:otherwise>
</c:choose>
<br/>
<html:link action="/results">
  <fmt:message key="results.returnToResults"/>
</html:link>
<!-- /objectDetailsPage.jsp -->
