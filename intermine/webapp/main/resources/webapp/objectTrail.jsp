<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<!-- objectTrail.jsp -->

<html:xhtml/>
<c:if test="${!empty trailElements && templateQuery.name != WEB_PROPERTIES['begin.browse.template']}">
  <div class="body objectTrail">
    Trail: <html:link action="/query.do?showTemplate=true"
                 styleClass="objectTrailLinkResults">Query</html:link>
    &gt;
    <c:forEach items="${trailElements}" var="item" varStatus="status">
      <c:choose>
        <c:when test="${item.table}">
          <fmt:message key="objectTrail.results" var="resultsLabel"/>
          <html:link action="/results?table=${item.tableId}"
                 styleClass="objectTrailLinkResults">${resultsLabel}</html:link>
        </c:when>
        <c:otherwise>
          <html:link action="/objectDetails?id=${item.objectId}&amp;trail=${item.trail}"
                 styleClass="objectTrailLink" title="ID = ${item.objectId}">${item.label}</html:link>
        </c:otherwise>
      </c:choose>
      <c:if test="${!status.last}">
        &gt;
      </c:if>
    </c:forEach>
  </div>
</c:if>

<!-- /objectTrail.jsp -->

