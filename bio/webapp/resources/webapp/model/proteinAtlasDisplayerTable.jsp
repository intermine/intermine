<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<tiles:importAttribute name="rows"/>

<table class="graph">
  <thead>
    <tr>
      <th class="sortable" title="byLevel">Overall</th>
      <th class="sortable" title="byOrgan">Organ</th>
      <th class="sortable" title="byCells">Cell<br />types</th>
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
        <c:set var="expressions" value="${expressionList.values}"/>
        <td>${fn:length(expressions)}</td>
        <td>
          <a target="new" href="${url}">
            <div class="expressions">
              <c:forEach var="expression" items="${expressions}">
                <div class="expression ${fn:toLowerCase(expression.value.level)}"><span class="tissue">${expression.value.tissue} (${expression.value.cellType})</span></div>
              </c:forEach>
            </div>
          </a>
        </td>
      </tr>
    </c:forEach>
  </tbody>
</table>