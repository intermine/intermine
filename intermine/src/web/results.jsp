<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="/WEB-INF/struts-bean-el.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<tiles:importAttribute/>

<html:form action="/changeResultsSize">

<%-- The following should probably be turned into a tag at some stage --%>
<table border="1px" width="90%">
  <%-- The headers --%>
  <tr>
    <c:forEach var="column" items="${resultsTable.columns}" varStatus="status">
      <th colspan=2 align="center" class="resultsHeader">
        <c:out value="${column.alias}"/>

        <%-- order by --%>
        <html:link action="/changeResults?method=orderByColumn&columnAlias=${column.alias}">
          [<bean:message key="results.orderByColumn"/>]
        </html:link>

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
  <c:if test="${resultsTable.size > 0}">
    <c:forEach var="row" items="${resultsTable.results}" varStatus="status" begin="${resultsTable.start}" end="${resultsTable.end}">

      <c:set var="rowClass">
        <c:choose>
          <c:when test="${status.count % 2 == 1}">resultsOddRow</c:when>
          <c:otherwise>resultsEvenRow</c:otherwise>
        </c:choose>
      </c:set>

      <tr class="<c:out value="${rowClass}"/>">

        <c:forEach var="column" items="${resultsTable.columns}" varStatus="status2">
          <c:choose>  
            <c:when test="${column.visible}">
              <%-- the checkbox to select this object --%>
              <td align="center">
                <html:multibox property="selectedObjects">
                  <c:out value="${status.index}"/>
                </html:multibox>
                <c:out value="${column.index}"/>,<c:out value="${status.index}"/>
              </td>
              <td>
                <c:out value="${row[column.index]}"/>
              </td>
            </c:when>
            <c:otherwise>
              <td colspan=2></td>
            </c:otherwise>
          </c:choose>
        </c:forEach>
      </tr>
    </c:forEach>
  </c:if>

  <%-- The footers --%>
  <tr class="resultsFooter">
    <c:forEach var="column" items="${resultsTable.columns}" varStatus="status">
      <td align="center">
        <html:submit property="action">
          <bean:message key="button.save"/>
        </html:submit>
      </td>
      <td></td>
    </c:forEach>
  </tr>
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
<br/>

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
<br/>
<%-- Page size controls --%>

  <bean:message key="results.changepagesize"/>
  <html:select property="pageSize">
    <html:option value="10">10</html:option>
    <html:option value="25">25</html:option>
    <html:option value="50">50</html:option>
    <html:option value="100">100</html:option>
  </html:select>
  <html:submit property="action">
    <bean:message key="button.change"/>
  </html:submit>

</html:form>
