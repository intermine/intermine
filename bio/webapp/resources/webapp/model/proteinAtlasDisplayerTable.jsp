<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<tiles:importAttribute name="rows"/>

<table class="graph">
  <thead>
    <tr>
      <th class="sortable" title="level">Overall</th>
      <th class="sortable" title="organ">Organ</th>
      <th class="sortable" title="cells">Cells</th>
      <th>Antibody staining</th>
    </tr>
  </thead>
  <tbody>
    <c:forEach var="organ" items="${rows}" varStatus="status">
      <tr class="organ<c:if test='${status.count % 2 == 0}'> alt</c:if>">
        <c:set var="expressionList" value="${organ.value}"/>
        <td>
          <c:set var="stainingLevel" value="${expressionList.stainingLevel}"/>
          <span class="level ${stainingLevel.levelClass}"><span class="value">${stainingLevel.levelValue}</span></span>
        </td>
        <td><span class="name">${expressionList.organName}</span></td>
        <c:set var="cellTypesCount" value="${fn:length(expressionList)}"/>
        <td>${cellTypesCount}</td>
        <td>
          <div class="expressions">
            <c:forEach begin="${1}" end="${cellTypesCount}">
              <c:set var="expression" value="${expressionList.item}"/>
              <div class="expression ${fn:toLowerCase(expression.level)}"><span class="tissue">${expression.tissue}</span></div>
            </c:forEach>
          </div>
        </td>
      </tr>
    </c:forEach>
  </tbody>
</table>