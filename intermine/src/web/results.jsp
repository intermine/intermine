<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="/WEB-INF/struts-bean-el.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<tiles:importAttribute/>

<%-- The following should probably be turned into a tag at some stage --%>
<table border="1px" width="90%">
  <%-- The headers --%>
  <tr>
    <c:forEach var="column" items="${resultsTable.columns}" varStatus="status">
      <c:if test="${resultsTable.columns[status.index].visible}">
        <th align="center" class="resultsHeader">
          <c:out value="${column.alias}"/>
        </th>
      </c:if>
    </c:forEach>
  </tr>

  <%-- The data --%>

  <%-- Row --%>
  <c:forEach var="row" items="${results}" varStatus="status" begin="${resultsTable.start}" end="${resultsTable.end}">

    <c:set var="rowClass">
      <c:choose>
        <c:when test="${status.count % 2 == 1}">resultsOddRow</c:when>
        <c:otherwise>resultsEvenRow</c:otherwise>
      </c:choose>
    </c:set>

    <tr class="<c:out value="${rowClass}"/>">
      <c:forEach var="item" items="${row}">
        <td>
          <c:out value="${item}"/>
        </td>
      </c:forEach>
    </tr>
  </c:forEach>

</table>

<%-- "Displaying xxx to xxx of xxx rows" messages --%>
<c:choose>
  <c:when test="${resultsTable.sizeEstimate}">
    <bean:message key="results.pageinfo.estimate"
                  arg0="${resultsTable.start+1}"
                  arg1="${resultsTable.end+1}"
                  arg2="${resultsTable.size}"/>
  </c:when>
  <c:otherwise>
    <bean:message key="results.pageinfo.exact"
                  arg0="${resultsTable.start+1}"
                  arg1="${resultsTable.end+1}"
                  arg2="${resultsTable.size}"/>
  </c:otherwise>

</c:choose>

<%-- Paging controls --%>
<html:link action="/changeResults?method=first">
  <bean:message key="results.first"/>
</html:link>
<c:if test="${resultsTable.previousButton}">
  <html:link action="/changeResults?method=previous">
    <bean:message key="results.previous"/>
  </html:link>
</c:if>
<c:if test="${resultsTable.nextButton}">
  <html:link action="/changeResults?method=next">
    <bean:message key="results.next"/>
  </html:link>
</c:if>
<html:link action="/changeResults?method=last">
  <bean:message key="results.last"/>
</html:link>
