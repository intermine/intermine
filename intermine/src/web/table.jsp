<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<tiles:importAttribute/>

<!-- table.jsp -->

<script>
function selectColumnCheckboxes(column) {
  var columnCheckBox = 'selectedObjects_' + column;
  with(document.changeResultsForm) {
    for(i=0;i < elements.length;i++) {
      thiselm = elements[i];
      var testString = 'selectedObjects_' + column + '_';
      if(thiselm.id.indexOf(testString) != -1)
        thiselm.checked = document.getElementById(columnCheckBox).checked;
    }
  }
}
function unselectColumnCheckbox(column) {
  document.getElementById('selectedObjects_' + column).checked = false;
}
</script>

<html:form action="/changeResultsSize" styleId="changeResultsForm">

  <%-- The following should probably be turned into a tag at some stage --%>
  <table class="results" cellspacing="0" width="1%">
    <%-- The headers --%>
    <tr>
      <c:forEach var="column" items="${RESULTS_TABLE.columns}" varStatus="status">
        <th align="center" width="1%">
          <html:multibox property="selectedObjects" styleId="selectedObjects_${status.index}"
                         onclick="selectColumnCheckboxes(${status.index})">
            <c:out value="${status.index}"/>
          </html:multibox>
        </th>

        <th align="center" width="1%">
          <div>
            <nobr>
              <c:out value="${column.name}"/>
            </nobr>
          </div>
          <div>
            <nobr>
              <%-- right/left --%>
              <c:if test="${not status.first}">
                [<html:link action="/changeResults?method=moveColumnLeft&index=${status.index}">
                  <fmt:message key="results.moveLeft"/>
                </html:link>]
              </c:if>
              <c:if test="${not status.last}">
                [<html:link action="/changeResults?method=moveColumnRight&index=${status.index}">
                  <fmt:message key="results.moveRight"/>
                </html:link>]
              </c:if>

              <%-- show/hide --%>
              <c:choose>
                <c:when test="${column.visible}">
                  <c:if test="${RESULTS_TABLE.visibleColumnCount > 1}">
                    [<html:link action="/changeResults?method=hideColumn&index=${status.index}">
                      <fmt:message key="results.hideColumn"/>
                    </html:link>]
                  </c:if>
                </c:when>
                <c:otherwise>
                  [<html:link action="/changeResults?method=showColumn&index=${status.index}">
                    <fmt:message key="results.showColumn"/>
                  </html:link>]
                </c:otherwise>
              </c:choose>
            </nobr>
          </div>
        </th>
      </c:forEach>
    </tr>

    <%-- The data --%>

    <%-- Row --%>
    <c:if test="${RESULTS_TABLE.estimatedSize > 0}">
      <c:forEach var="row" items="${RESULTS_TABLE.list}" varStatus="status"
                 begin="${RESULTS_TABLE.start}" end="${RESULTS_TABLE.end}">

        <c:set var="rowClass">
          <c:choose>
            <c:when test="${status.count % 2 == 1}">odd</c:when>
            <c:otherwise>even</c:otherwise>
          </c:choose>
        </c:set>

        <tr class="<c:out value="${rowClass}"/>">
          <c:forEach var="column" items="${RESULTS_TABLE.columns}" varStatus="status2">
            <c:choose>
              <c:when test="${column.visible}">
                <c:choose>
                  <%-- "!=" only works on objects of the same type.  Objects in a
                       column will only have the same type if we are showing a
                       collection.  Collections will have only one column so
                       don't do the check if there is only one column --%>
                  <c:when test="${RESULTS_TABLE.tableWidth == 1 ||
                                  ((status.count == 1) ||
                                   (row[status2.index] != prevrow[status2.index]))}">
                    <%-- the checkbox to select this object --%>
                    <td align="center" width="1%">
                      <html:multibox property="selectedObjects"
                                     styleId="selectedObjects_${status2.index}_${status.index}"
                                     onclick="unselectColumnCheckbox(${status2.index})">
                        <c:out value="${status2.index},${status.index}"/>
                      </html:multibox>
                    </td>
                    <td>
                      <c:set var="object" value="${row[column.index]}" scope="request"/>
                      <tiles:get name="objectSummary.tile" />
                    </td>
                  </c:when>
                  <c:otherwise>
                    <%-- add a space so that IE renders the borders --%>
                    <td colspan="2">&nbsp;</td>
                  </c:otherwise>
                </c:choose>
              </c:when>
              <c:otherwise>
                <td colspan="2">&nbsp;</td>
              </c:otherwise>
            </c:choose>
          </c:forEach>
        </tr>
        <c:set var="prevrow" value="${row}"/>
      </c:forEach>
    </c:if>
  </table>
  <br/>

  <%-- "Displaying xxx to xxx of xxx rows" messages --%>
  <c:choose>
    <c:when test="${RESULTS_TABLE.estimatedSize == 0}">
      <fmt:message key="results.pageinfo.empty"/>
    </c:when>
    <c:when test="${RESULTS_TABLE.sizeEstimate}">
      <fmt:message key="results.pageinfo.estimate">
        <fmt:param value="${RESULTS_TABLE.start+1}"/>
        <fmt:param value="${RESULTS_TABLE.end+1}"/>
        <fmt:param value="${RESULTS_TABLE.estimatedSize}"/>
      </fmt:message>
    </c:when>
    <c:otherwise>
      <fmt:message key="results.pageinfo.exact">
        <fmt:param value="${RESULTS_TABLE.start+1}"/>
        <fmt:param value="${RESULTS_TABLE.end+1}"/>
        <fmt:param value="${RESULTS_TABLE.exactSize}"/>
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
  <c:if test="${RESULTS_TABLE.previousRows}">
    <html:link action="/changeResults?method=previous">
      <fmt:message key="results.previous"/>
    </html:link>
  </c:if>
  <c:if test="${RESULTS_TABLE.moreRows}">
    <html:link action="/changeResults?method=next">
      <fmt:message key="results.next"/>
    </html:link>
  </c:if>
  <c:if test="${RESULTS_TABLE.sizeEstimate || (RESULTS_TABLE.end != RESULTS_TABLE.exactSize - 1)}">
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
    <html:option value="200">200</html:option>
  </html:select>
  <html:submit property="buttons(changePageSize)">
    <fmt:message key="button.change"/>
  </html:submit>
  <br/>

  <%-- Save bag controls --%>
  <br/>
  <c:if test="${RESULTS_TABLE.estimatedSize > 0}">
    <c:if test="${!empty SAVED_BAGS}">
      <html:select property="bagName">

        <c:forEach items="${SAVED_BAGS}" var="entry">
          <html:option value="${entry.key}"/>
        </c:forEach>
      </html:select>
      <html:submit property="buttons(addToExistingBag)">
        <fmt:message key="bag.existing"/>
      </html:submit>
      <br/>
      <br/>
    </c:if>

    <html:text property="newBagName"/>
    <html:submit property="buttons(saveNewBag)">
      <fmt:message key="bag.new"/>
    </html:submit>
    <br/>
  </c:if>
</html:form>

<div>
  <html:link action="/exportAction?method=excel"><fmt:message key="export.excel"/></html:link>
</div>
<div>
  <html:link action="/exportAction?method=csv"><fmt:message key="export.csv"/></html:link>
</div>
<div>
  <html:link action="/exportAction?method=tab"><fmt:message key="export.tabdelimited"/></html:link>
</div>
<!-- /table.jsp -->
