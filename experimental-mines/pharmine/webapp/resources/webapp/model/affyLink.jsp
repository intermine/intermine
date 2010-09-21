<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- imageDisplayer.jsp -->
<c:if test="${!empty object.probeSets}">
  <c:forEach items="${object.probeSets}" var="thisSet">
    <html:img src="http://biogps.gnf.org/service/datasetchart/?datarowname=${thisSet.focusIdentifier}&datasetid=1&binary=t&imagetype=fs&imageSize=370"/>
  </c:forEach>
</c:if>
<!-- /imageDisplayer.jsp -->
