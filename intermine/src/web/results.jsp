<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<tiles:importAttribute/>

<!-- results.jsp -->
<html:form action="/changeResultsSize">

  <%-- The following should probably be turned into a tag at some stage --%>
  <table border="1px" width="100%">
    <%-- The headers --%>
    <tr>
      <c:forEach var="column" items="${RESULTS_TABLE.columns}" varStatus="status">
        <th colspan=2 align="center" class="resultsHeader">
          <c:out value="${column.alias}"/>

          <%-- order by --%>
          <html:link action="/changeResults?method=orderByColumn&columnAlias=${column.alias}">
            [<fmt:message key="results.orderByColumn"/>]
          </html:link>

          <%-- show/hide --%>
          <c:choose>
            <c:when test="${column.visible}">
              <html:link action="/changeResults?method=hideColumn&columnAlias=${column.alias}">
                [<fmt:message key="results.hideColumn"/>]
              </html:link>
            </c:when>
            <c:otherwise>
              <html:link action="/changeResults?method=showColumn&columnAlias=${column.alias}">
                [<fmt:message key="results.showColumn"/>]
              </html:link>
            </c:otherwise>
          </c:choose>

          <%-- right/left --%>
          <c:if test="${not status.first}">
            <html:link action="/changeResults?method=moveColumnUp&columnAlias=${column.alias}">
              [<fmt:message key="results.moveUp"/>]
            </html:link>
          </c:if>
          <c:if test="${not status.last}">
            <html:link action="/changeResults?method=moveColumnDown&columnAlias=${column.alias}">
              [<fmt:message key="results.moveDown"/>]
            </html:link>
          </c:if>
        </th>
      </c:forEach>
    </tr>

    <%-- The data --%>

    <%-- Row --%>
    <c:if test="${RESULTS_TABLE.size > 0}">
      <c:forEach var="row" items="${RESULTS_TABLE.results}" varStatus="status" begin="${RESULTS_TABLE.start}" end="${RESULTS_TABLE.end}">

        <c:set var="rowClass">
          <c:choose>
            <c:when test="${status.count % 2 == 1}">resultsOddRow</c:when>
            <c:otherwise>resultsEvenRow</c:otherwise>
          </c:choose>
        </c:set>

        <tr class="<c:out value="${rowClass}"/>">
          <c:forEach var="column" items="${RESULTS_TABLE.columns}" varStatus="status2">
            <c:choose>
              <c:when test="${column.visible}">
                <c:choose>
                  <c:when test="${(status.count == 1) || (row[column.index] != prevrow[column.index])}">
                    <%-- the checkbox to select this object --%>
                    <td align="center" width="1">
                      <html:multibox property="selectedObjects">
                        <c:out value="${column.index},${status.index}"/>
                      </html:multibox>
                    </td>
                    <td>
                      <c:set var="object" value="${row[column.index]}" scope="request"/>
                      <tiles:get name="resultsCell.tile" />
                    </td>
                  </c:when>
                  <c:otherwise>
                    <td colspan=2/>
                  </c:otherwise>
                </c:choose>
              </c:when>
              <c:otherwise>
                <td colspan=2></td>
              </c:otherwise>
            </c:choose>
          </c:forEach>
        </tr>
        <c:set var="prevrow" value="${row}"/>
      </c:forEach>
    </c:if>
  </table>

  <%-- "Displaying xxx to xxx of xxx rows" messages --%>
  <c:choose>
    <c:when test="${RESULTS_TABLE.size == 0}">
      <fmt:message key="results.pageinfo.empty"/>
    </c:when>
    <c:when test="${RESULTS_TABLE.sizeEstimate}">
      <fmt:message key="results.pageinfo.estimate">
        <fmt:param value="${RESULTS_TABLE.start+1}"/>
        <fmt:param value="${RESULTS_TABLE.end+1}"/>
        <fmt:param value="${RESULTS_TABLE.size}"/>
      </fmt:message>
    </c:when>
    <c:otherwise>
      <fmt:message key="results.pageinfo.exact">
        <fmt:param value="${RESULTS_TABLE.start+1}"/>
        <fmt:param value="${RESULTS_TABLE.end+1}"/>
        <fmt:param value="${RESULTS_TABLE.size}"/>
      </fmt:message>
    </c:otherwise>
  </c:choose>
  <br/>

  <%-- Paging controls --%>
  <c:if test="${RESULTS_TABLE.start > 0}">
    <html:link action="/changeResults?method=first">
      <fmt:message key="results.first"/>
    </html:link>
  </c:if>
  <c:if test="${RESULTS_TABLE.previousButton}">
    <html:link action="/changeResults?method=previous">
      <fmt:message key="results.previous"/>
    </html:link>
  </c:if>
  <c:if test="${RESULTS_TABLE.nextButton}">
    <html:link action="/changeResults?method=next">
      <fmt:message key="results.next"/>
    </html:link>
  </c:if>
  <c:if test="${RESULTS_TABLE.sizeEstimate || (RESULTS_TABLE.end != RESULTS_TABLE.size - 1)}">
    <html:link action="/changeResults?method=last">
      <fmt:message key="results.last"/>
    </html:link>
  </c:if>
  <br/>

  <%-- Page size controls --%>

  <fmt:message key="results.changepagesize"/>
  <html:select property="pageSize">
    <html:option value="10">10</html:option>
    <html:option value="25">25</html:option>
    <html:option value="50">50</html:option>
    <html:option value="100">100</html:option>
  </html:select>
  <html:submit property="action">
    <fmt:message key="button.change"/>
  </html:submit>
  <br/>
  <br/>

  <%-- Save bag controls --%>
  <br/>
  <c:if test="${RESULTS_TABLE.size > 0}">
    <c:if test="${!empty SAVED_BAGS}">
      <html:select property="bagName">

        <c:forEach items="${SAVED_BAGS}" var="entry">
          <html:option value="${entry.key}"/>
        </c:forEach>
      </html:select>
      <html:submit property="action">
        <fmt:message key="bag.existing"/>
      </html:submit>
      <br/>
    </c:if>

    <html:text property="newBagName"/>
    <html:submit property="action">
      <fmt:message key="bag.new"/>
    </html:submit>
    <br/>
  </c:if>
</html:form>

<tiles:get name="saveQuery"/>

<html:link action="/buildquery">Return to query</html:link>
<!-- /results.jsp -->
