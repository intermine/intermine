<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<tiles:importAttribute/>

<!-- table.jsp -->
<script type="text/javascript">
  <!--//<![CDATA[
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
    //]]>-->
</script>

<c:choose>
  <c:when test="${RESULTS_TABLE.size == 0}">
    <div class="body">
      <fmt:message key="results.pageinfo.empty"/><br/>
    </div>
  </c:when>
  <c:otherwise>
    <div class="body">
    <html:form action="/changeResultsSize" styleId="changeResultsForm">

      <table class="results" cellspacing="0">
        <%-- The headers --%>
        <tr>
          <c:forEach var="column" items="${RESULTS_TABLE.columns}" varStatus="status">
            <th align="center">
              <html:multibox property="selectedObjects" styleId="selectedObjects_${status.index}"
                             onclick="selectColumnCheckboxes(${status.index})">
                <c:out value="${status.index}"/>
              </html:multibox>
            </th>

            <th align="center">
              <div>
                <nobr>
                  <c:out value="${column.name}"/>
                </nobr>
              </div>
              <div>
                <nobr>
                  <%-- right/left --%>
                  <c:if test="${not status.first}">
                    <fmt:message key="results.moveLeftHelp" var="moveLeftTitle">
                      <fmt:param value="${column.name}"/>
                    </fmt:message>
                    [
                    <html:link action="/changeResults?method=moveColumnLeft&index=${status.index}"
                               title="${moveLeftTitle}">
                      <fmt:message key="view.moveLeftSymbol"/>
                    </html:link>
                    ]
                  </c:if>
                  <c:if test="${not status.last}">
                    <fmt:message key="results.moveRightHelp" var="moveRightTitle">
                      <fmt:param value="${column.name}"/>
                    </fmt:message>
                    [
                    <html:link action="/changeResults?method=moveColumnRight&index=${status.index}"
                               title="${moveRightTitle}">
                      <fmt:message key="view.moveRightSymbol"/>
                    </html:link>
                    ]
                  </c:if>

                  <%-- show/hide --%>
                  <c:choose>
                    <c:when test="${column.visible}">
                      <c:if test="${RESULTS_TABLE.visibleColumnCount > 1}">
                        <fmt:message key="results.hideColumnHelp" var="hideColumnTitle">
                          <fmt:param value="${column.name}"/>
                        </fmt:message>
                        [
                        <html:link action="/changeResults?method=hideColumn&index=${status.index}"
                                   title="${hideColumnTitle}">
                          <fmt:message key="results.hideColumn"/>
                        </html:link>
                        ]
                      </c:if>
                    </c:when>
                    <c:otherwise>
                      <fmt:message key="results.showColumnHelp" var="showColumnTitle">
                        <fmt:param value="${column.name}"/>
                      </fmt:message>
                      [
                      <html:link action="/changeResults?method=showColumn&index=${status.index}"
                                 title="${showColumnTitle}">
                        <fmt:message key="results.showColumn"/>
                      </html:link>
                      ]
                    </c:otherwise>
                  </c:choose>
                </nobr>
              </div>
            </th>
          </c:forEach>
        </tr>

        <%-- The data --%>

        <%-- Row --%>
        <c:if test="${RESULTS_TABLE.size > 0}">
          <c:forEach var="row" items="${RESULTS_TABLE.rows}" varStatus="status">

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
                    <%-- the checkbox to select this object --%>
                    <td align="center">
                      <html:multibox property="selectedObjects"
                                     styleId="selectedObjects_${status2.index}_${status.index}"
                                     onclick="unselectColumnCheckbox(${status2.index})">
                        <c:out value="${status2.index},${status.index}"/>
                      </html:multibox>
                    </td>
                    <td>
                      <c:set var="object" value="${row[column.index]}" scope="request"/>
                      <c:choose>
                        <c:when test="${RESULTS_TABLE.summary}">
                          <c:set var="viewType" value="summary" scope="request"/>
                        </c:when>
                        <c:otherwise>
                          <c:set var="viewType" value="detail" scope="request"/>
                        </c:otherwise>
                      </c:choose>
                      <tiles:get name="objectView.tile" />
                    </td>
                  </c:when>
                  <c:otherwise>
                    <%-- add a space so that IE renders the borders --%>
                    <td colspan="2">&nbsp;</td>
                  </c:otherwise>
                </c:choose>
              </c:forEach>
            </tr>
          </c:forEach>
        </c:if>
      </table>

      <c:if test="${RESULTS_TABLE.size > 1}">
        <%-- "Displaying xxx to xxx of xxx rows" messages --%>
        <br/>
        <c:choose>
          <c:when test="${RESULTS_TABLE.sizeEstimate}">
            <fmt:message key="results.pageinfo.estimate">
              <fmt:param value="${RESULTS_TABLE.startRow+1}"/>
              <fmt:param value="${RESULTS_TABLE.endRow+1}"/>
              <fmt:param value="${RESULTS_TABLE.size}"/>
            </fmt:message>
          </c:when>
          <c:otherwise>
            <fmt:message key="results.pageinfo.exact">
              <fmt:param value="${RESULTS_TABLE.startRow+1}"/>
              <fmt:param value="${RESULTS_TABLE.endRow+1}"/>
              <fmt:param value="${RESULTS_TABLE.size}"/>
            </fmt:message>
          </c:otherwise>
        </c:choose>
        <br/>
        
        <%-- Paging controls --%>
        <c:if test="${!RESULTS_TABLE.firstPage}">
          <html:link action="/changeResults?method=first">
            <fmt:message key="results.first"/>
          </html:link>
          <html:link action="/changeResults?method=previous">
            <fmt:message key="results.previous"/>
          </html:link>
        </c:if>
        <c:if test="${!RESULTS_TABLE.lastPage}">
          <html:link action="/changeResults?method=next">
            <fmt:message key="results.next"/>
          </html:link>
          <c:if test="${RESULTS_TABLE.maxRetrievableIndex > RESULTS_TABLE.size}">
            <html:link action="/changeResults?method=last">
              <fmt:message key="results.last"/>
            </html:link>
          </c:if>
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
        <html:submit property="changePageSize">
          <fmt:message key="button.change"/>
        </html:submit>
        <br/>
      </c:if>

      <%-- Return to main results link --%>
      <c:if test="${RESULTS_TABLE.class.name != 'org.intermine.web.results.PagedResults' && QUERY_RESULTS != null && empty bagName}">
        <br>
        <html:link action="/changeResults?method=reset">
          <fmt:message key="results.return"/>
        </html:link>
      </c:if>
    
      </div> <%-- end of main results table body div --%>
      
      <%-- Save bag controls --%>
      <br/><br/>
      <c:if test="${RESULTS_TABLE.size > 0}">
        <div class="heading">
          <fmt:message key="results.save"/>
        </div>
        <div class="body">
          <ul>
            <li>
              <fmt:message key="bag.new"/>
              <html:text property="newBagName"/>
              <html:submit property="saveNewBag">
                <fmt:message key="button.save"/>
              </html:submit>
            </li>
            <c:if test="${!empty PROFILE.savedBags}">
              <li>
                <fmt:message key="bag.existing"/>
                <html:select property="existingBagName">
                  <c:forEach items="${PROFILE.savedBags}" var="entry">
                    <html:option value="${entry.key}"/>
                  </c:forEach>
                </html:select>
                <html:submit property="addToExistingBag">
                  <fmt:message key="button.add"/>
                </html:submit>
              </li>
            </c:if>
          </ul>
        </div>
      </c:if>
    </html:form>

    <tiles:get name="export.tile" />
  </c:otherwise>
</c:choose>
<!-- /table.jsp -->
