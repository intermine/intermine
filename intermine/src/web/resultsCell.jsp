<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<tiles:importAttribute scope="request"/>

<!-- resultsCell.jsp -->
<c:choose>
  <%-- check whether we have a business object or a plain java object --%>
  <c:when test="${!empty leafClds}">
    <tiles:insert name="objectSummary.tile"/>
  </c:when>
  <c:otherwise>
    <font class="resultsCellValue">
      <c:out value="${object}"/>
    </font>
  </c:otherwise>
</c:choose>
<!-- /resultsCell.jsp -->
