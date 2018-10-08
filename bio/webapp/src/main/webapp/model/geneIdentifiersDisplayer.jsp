<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>

<c:if test="${!empty identifiers}">
  <div id="gene-identifiers-displayer" class="inline-list">
  <ul>
  <li><span class="name label">identifiers:</span></li>
  <c:set var="size" value="${fn:length(identifiers)}" />
  <c:forEach var="identifier" items="${identifiers}" varStatus="status">
    <li><c:out value="${identifier.value}" /><c:if test="${status.count < size}">, </c:if></li>
  </c:forEach>
  </ul>
  </div>
</c:if>