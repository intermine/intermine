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
      <th align="center" class="resultsHeader">
        <c:out value="${column.alias}"/>

        <%-- show/hide --%>
        <c:choose>
          <c:when test="${column.visible}">
            <html:link action="/changeResults?method=hideColumn&columnAlias=${column.alias}">
              [<bean:message key="results.hideColumn"/>]
            </html:link>
          </c:when>
          <c:otherwise>
            <html:link action="/changeResults?method=showColumn&columnAlias=${column.alias}">
              [<bean:message key="results.showColumn"/>]
            </html:link>
          </c:otherwise>
        </c:choose>

        <%-- right/left --%>
        <c:if test="${not status.first}">
          <html:link action="/changeResults?method=moveColumnUp&columnAlias=${column.alias}">
            [<bean:message key="results.moveUp"/>]
          </html:link>
        </c:if>
        <c:if test="${not status.last}">
          <html:link action="/changeResults?method=moveColumnDown&columnAlias=${column.alias}">
            [<bean:message key="results.moveDown"/>]
          </html:link>
        </c:if>
      </th>
    </c:forEach>
  </tr>

  <%-- The data --%>

  <%-- Row --%>
  <c:forEach var="row" items="${resultsTable.results}" varStatus="status" begin="${resultsTable.start}" end="${resultsTable.end}">

    <c:set var="rowClass">
      <c:choose>
        <c:when test="${status.count % 2 == 1}">resultsOddRow</c:when>
        <c:otherwise>resultsEvenRow</c:otherwise>
      </c:choose>
    </c:set>

    <tr class="<c:out value="${rowClass}"/>">

      <c:forEach var="column" items="${resultsTable.columns}">
        <td>
          <c:if test="${column.visible}">
            <c:out value="${row[column.index]}"/>
          </c:if>
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
<c:if test="${resultsTable.start > 0}">
  <html:link action="/changeResults?method=first">
    <bean:message key="results.first"/>
  </html:link>
</c:if>
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
<c:if test="${resultsTable.sizeEstimate || (resultsTable.end != resultsTable.size - 1)}">
  <html:link action="/changeResults?method=last">
    <bean:message key="results.last"/>
  </html:link>
</c:if>
