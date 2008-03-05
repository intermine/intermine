<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<!-- table.jsp -->

<tiles:importAttribute/>
<html:xhtml/>
<link rel="stylesheet" href="css/resultstables.css" type="text/css" />
<tiles:get name="objectTrail.tile"/> <%--<im:vspacer height="1"/>--%>

<%-- PagedTable.getWebTableClass() is a bit hacky - replace with a boolean
     method or make it unnecessary --%>
<c:set var="isWebResults"
       value="${resultsTable.webTableClass.name == 'org.intermine.web.logic.results.WebResults'}"/>
<c:set var="isWebCollection"
       value="${resultsTable.webTableClass.name == 'org.intermine.web.struts.WebPathCollection'}"/>
<c:set var="noBagSave"
       value="${!empty param.noSelect}"/>

<script type="text/javascript">
<!--//<![CDATA[
  function changePageSize() {
    var url = '${requestScope['javax.servlet.include.context_path']}/results.do?';
    var pagesize = document.changeTableSizeForm.pageSize.options[document.changeTableSizeForm.pageSize.selectedIndex].value;
    var page = ${resultsTable.startRow}/pagesize;
    url += 'table=${param.table}' + '&page=' + Math.floor(page) + '&size=' + pagesize;
    if ('${param.trail}' != '') {
        url += '&trail=${param.trail}';
    }
    document.location.href=url;
  }

  var columnsToDisable = ${columnsToDisable};
  var columnsToHighlight = ${columnsToHighlight};
  var bagType = null;  

//]]>-->
</script>
<script type="text/javascript" src="js/table.js" ></script>


    <c:if test="${!empty templateQuery || !empty param.templateQueryTitle}">

      <%-- show the description only if we ve run a query (rather than viewing
           a bag) - see #1031 --%>
      <c:if test="${isWebResults
                  && (pathQuery.name != WEB_PROPERTIES['begin.browse.template'])}">
        <div class="body">
          <div class="resultsTableTemplateHeader">
            <div>
              <fmt:message key="results.templateTitle"/>:
              <span class="templateTitleBold"> <c:choose>
              
                <c:when test="${!empty param.templateQueryTitle}">
                 ${param.templateQueryTitle}
                 </c:when>
                 <c:otherwise>
                ${templateQuery.title}
                 </c:otherwise>
                 </c:choose>
              </span>
            </div>
            <div class="templateDescription">
             <c:choose>
              
                <c:when test="${!empty param.templateQueryTitle}">
                 ${param.templateQueryDescription}
                 </c:when>
                 <c:otherwise>
                ${templateQuery.description}
                 </c:otherwise>
                 </c:choose>
            </div>
          </div>
        </div>
      </c:if>
    </c:if>

<c:choose>
  <c:when test="${resultsTable.estimatedSize == 0}">
    <div class="altmessage">
      <fmt:message key="results.pageinfo.empty"/><br/>
    </div>
  </c:when>
  <c:otherwise>


<div class="body">

<%-- Toolbar --%>
<link rel="stylesheet" href="css/toolbar.css" type="text/css" />
<link rel="stylesheet" href="css/tablePageLinks.css" type="text/css" >
<script type="text/javascript" src="js/toolbar.js"></script>

<div id="tool_bar_div">
<ul id="button_bar" onclick="toggleToolBarMenu(event);">
<li id="tool_bar_li_createlist"><img style="cursor: pointer;" src="images/icons/null.gif" width="90" height="25" alt="Create List" border="0" id="tool_bar_button_createlist" class="tool_bar_button"></li>
<li id="tool_bar_li_addtolist"><img style="cursor: pointer;" src="images/icons/null.gif" width="91" height="25" alt="Add to List" border="0" id="tool_bar_button_addtolist" class="tool_bar_button"></li>
<li id="tool_bar_li_export"><img style="cursor: pointer;" src="images/icons/null.gif" width="64" height="25" alt="Export" border="0" id="tool_bar_button_export" class="tool_bar_button"></li>
<li class="tool_bar_separator"><span>&nbsp;//&nbsp;</span></li>
<li class="tool_bar_link">
<html:form action="/changeTableSize">
  
  <%-- Page size controls --%>
  <span style="float:left;padding:4px 5px 0 10px;"><fmt:message key="results.changepagesize"/></span>
    <html:select property="pageSize" onchange="changePageSize()" value="${resultsTable.pageSize}">
    <html:option value="10">10</html:option>
    <html:option value="25">25</html:option>
    <html:option value="50">50</html:option>
    <html:option value="100">100</html:option>
  </html:select>
  <input type="hidden" name="table" value="${param.table}"/>
  <input type="hidden" name="trail" value="${param.trail}"/>
  <noscript>
    <html:submit>
      <fmt:message key="button.change"/>
    </html:submit>
  </noscript>
</html:form>
</li>
<li class="tool_bar_link">
    <tiles:insert page="/tablePageLinks.jsp">
      <tiles:put name="short" value="true" />
      <tiles:put name="currentPage" value="results" />
    </tiles:insert>
</li>
</ul>
</div>

<%-- Create new list --%>
<html:form action="/saveBag" >
<input type="hidden" name="operationButton"/>

<div id="tool_bar_item_createlist" style="visibility:hidden" class="tool_bar_item">
      <em>(with selected items)</em>
      <fmt:message key="bag.new"/><br/>
      <input type="text" name="newBagName" id="newBagName" onkeypress="javascript:onSaveBagEnter('saveBagForm')"/>
      <input type="hidden" name="__intermine_forward_params__" value="${pageContext.request.queryString}"/>
      <input type="hidden" name="table" value="${param.table}"/>
      <input type="button" name="saveNewBag" value="Save selected" id="saveNewBag" onclick="javascript:validateBagName('saveBagForm');"/>
      <script type="text/javascript" charset="utf-8">
        $('newBagName').disabled = true;
        $('saveNewBag').disabled = true;
      </script>
    <hr>
  <a href="javascript:hideMenu('tool_bar_item_createlist')" >Cancel</a>
</div>
<%-- Add to existing list --%>
<div id="tool_bar_item_addtolist" style="visibility:hidden" class="tool_bar_item">
   <c:choose>
   <c:when test="${!empty PROFILE.savedBags}">
          <fmt:message key="bag.existing"/>
          <html:select property="existingBagName">
             <c:forEach items="${PROFILE.savedBags}" var="entry">
              <c:if test="${param.bagName != entry.key}">
                <html:option value="${entry.key}"/>
              </c:if>
             </c:forEach>
          </html:select>              
     <input type="submit" name="addToBag" id="addToBag" value="Add selected" />
     <script type="text/javascript" charset="utf-8">
          $('addToBag').disabled = true;
        </script>
    </c:when>
    <c:otherwise>
      <em>no lists saved</em>
    </c:otherwise>
    </c:choose>
    <hr>
  <a href="javascript:hideMenu('tool_bar_item_addtolist')" >Cancel</a>
</div>
<%-- Export --%>
<div id="tool_bar_item_export" style="visibility:hidden" class="tool_bar_item">
    <c:set var="tableName" value="${param.table}" scope="request"/>
    <c:set var="tableType" value="results" scope="request"/>
    <c:set var="pagedTable" value="${resultsTable}" scope="request"/>
    <tiles:get name="export.tile"/>
    <hr>
  <a href="javascript:hideMenu('tool_bar_item_export')" >Cancel</a>
</div>

 <%--           <strong>Legend: </strong>
            <img style="vertical-align:text-bottom;" border="0"
                 width="13" height="13" src="images/left_arrow.png"
                 title="${moveLeftString}"/>
            &nbsp;&amp;&nbsp;<img style="vertical-align:text-bottom;" border="0"
                                  width="13" height="13" src="images/right_arrow.png"
                                  title="${moveLeftString}"/>
            Move columns - <img style="vertical-align:text-bottom;" border="0"
                                src="images/close.png" title="${hideColumnTitle}" />
            Close column <c:if test="${isWebResults}">-
            <img src="images/summary_maths.png" style="vertical-align:text-bottom;" title="Click here to view Column Summary statistics">
              Get summary statistics for column</img></c:if> 
 </div>--%>


<div class="resultsPage">

        <table class="results" cellspacing="0">

          <%-- The headers --%>
          <tr>
            <c:forEach var="column" items="${resultsTable.columns}" varStatus="status">
              <im:formatColumnName outVar="displayPath" str="${column.name}" />
              <im:unqualify className="${column.name}" var="pathEnd"/>
              <im:prefixSubstring str="${column.name}" outVar="columnPathPrefix" delimiter="."/>
              <c:choose>
                <c:when test="${!empty QUERY
                              && !empty QUERY.pathStringDescriptions[columnPathPrefix]}">
                  <c:set var="columnDisplayName" 
                         value="<span class='viewPathDescription' title='${displayPath}'>${QUERY.pathStringDescriptions[columnPathPrefix]}</span> &gt; ${pathEnd}"/>
                </c:when>
                <c:otherwise>
                  <c:set var="columnDisplayName" value="${displayPath}"/>
                </c:otherwise>
              </c:choose>
              <c:choose>
                <c:when test="${column.visible}">
                    <c:if test="${column.selectable && ((!isWebCollection) || (! noBagSave && status.count<=1))}">
                    <th align="center" class="checkbox">
                      <html:multibox property="selectedObjects" styleId="selectedObjects_${status.index}"
                                     onclick="selectColumnCheckbox(columnsToDisable, columnsToHighlight, ${status.index})"
                                     disabled="${resultsTable.maxRetrievableIndex > resultsTable.estimatedSize ? 'false' : 'true'}">
                        <c:out value="${status.index},${column.columnId}"/>
                      </html:multibox>
                    </th>
                  </c:if>

                  <th align="center" valign="top" >
                    <%-- put in left, right, hide and show buttons --%>
                    <div align="right" style="margin-right:0px;margin-top:0px;white-space:nowrap;">
                    
                   <%-- sort img --%>
                     <c:if test="${not empty sortOrderMap[column.name]}">
                          <img style="vertical-align:top;" border="0"
                               width="17" height="16" src="images/${sortOrderMap[column.name]}_gray.gif"
                                  title="Results are sorted by ${column.name}"/>
                      </c:if>      
                               
                      <%-- left --%>
                      <c:if test="${not status.first}">
                        <fmt:message key="results.moveLeftHelp" var="moveLeftTitle">
                          <fmt:param value="${column.name}"/>
                        </fmt:message>
                        <fmt:message key="results.moveLeftSymbol" var="moveLeftString"/>
                        <html:link action="/changeTable?table=${param.table}&amp;method=moveColumnLeft&amp;index=${status.index}&amp;trail=${param.trail}"
                                   title="${moveLeftTitle}">
                          <img style="vertical-align:top;" border="0"
                               width="13" height="13" src="images/left_arrow.png"
                               title="${moveLeftString}"/>
                        </html:link>
                      </c:if>

                      <%-- summary --%>
                      <c:if test="${isWebResults && !empty column.path.noConstraintsString}">
            <fmt:message key="columnsummary.getsummary" var="summaryTitle" />
                        <a href="javascript:getColumnSummary('${table}','${column.path.noConstraintsString}', &quot;${columnDisplayName}&quot;)" 
                           title="${summaryTitle}"><img src="images/summary_maths.png" title="${summaryTitle}"/></a>
                      </c:if>

                      <%-- right --%>
                      <c:if test="${not status.last}">
                        <fmt:message key="results.moveRightHelp" var="moveRightTitle">
                          <fmt:param value="${column.name}"/>
                        </fmt:message>
                        <fmt:message key="results.moveRightSymbol" var="moveRightString"/>
                        <html:link action="/changeTable?table=${param.table}&amp;method=moveColumnRight&amp;index=${status.index}&amp;trail=${param.trail}"
                                   title="${moveRightTitle}">
                          <img style="vertical-align:top;" border="0"
                               width="13" height="13"
                               src="images/right_arrow.png" title="${moveRightString}"/>
                        </html:link>
                      </c:if>

                      <%-- show/hide --%>
                      <c:if test="${fn:length(resultsTable.columns) > 1}">
                        <c:if test="${resultsTable.visibleColumnCount > 1}">
                          <fmt:message key="results.hideColumnHelp" var="hideColumnTitle">
                            <fmt:param value="${column.name}"/>
                          </fmt:message>
                          <html:link action="/changeTable?table=${param.table}&amp;method=hideColumn&amp;index=${status.index}&amp;trail=${param.trail}"
                                     title="${hideColumnTitle}">
                            <img style="vertical-align:top;" border="0"
                                 src="images/close.png" title="${hideColumnTitle}" />
                          </html:link>
                        </c:if>
                      </c:if>

                    </div>
                    <div>
                      <c:out value="${columnDisplayName}" escapeXml="false"/>
                    </div>
                  </th>
                </c:when>
                <c:otherwise>
                  <th>
                    <%-- <fmt:message key="results.showColumnHelp" var="showColumnTitle">
                         <fmt:param value="${column.name}"/>
                         </fmt:message> --%>
                    <html:link action="/changeTable?table=${param.table}&amp;method=showColumn&amp;index=${status.index}&amp;trail=${param.trail}"
                               title="${showColumnTitle}">
                      <img src="images/show-column.gif" title="${fn:replace(column.name, '.', '&nbsp;> ')}"/>
                    </html:link>
                  </th>
                </c:otherwise>
              </c:choose>
            </c:forEach>
          </tr>

          <%-- The data --%>

          <%-- Row --%>
          <c:if test="${resultsTable.estimatedSize > 0}">
            <c:forEach var="row" items="${resultsTable.resultElementRows}" varStatus="status">

              <c:set var="rowClass">
                <c:choose>
                  <c:when test="${status.count % 2 == 1}">odd</c:when>
                  <c:otherwise>even</c:otherwise>
                </c:choose>
              </c:set>

              <tr class="<c:out value="${rowClass}"/>">
                <c:forEach var="column" items="${resultsTable.columns}" varStatus="status2">

                  <c:choose>
                    <c:when test="${column.visible}">
                      <%-- the checkbox to select this object --%>
                       <c:if test="${column.selectable && ((!isWebCollection) || (! noBagSave && status2.count<=1))}">
                        <td align="center" class="checkbox" id="cell_checkbox,${status2.index},${status.index},${row[column.index].htmlId}">
                          <html:multibox property="selectedObjects"
                                         styleId="selectedObjects_${status2.index}_${status.index}_${row[column.index].htmlId}"
                                         onclick="itemChecked(columnsToDisable, columnsToHighlight, ${status.index},${status2.index}, this)">
                            <c:out value="${status2.index},${status.index},${row[column.index].htmlId}"/>
                          </html:multibox>
                        </td>
                      </c:if>
                      <td id="cell,${status2.index},${status.index},${row[column.index].htmlId}">
                        <c:set var="resultElement" value="${row[column.index]}" scope="request"/>
                        <c:set var="columnType" value="${column.type}" scope="request"/>
                        <tiles:insert name="objectView.tile" />
                      </td>
                    </c:when>
                    <c:otherwise>
                      <%-- add a space so that IE renders the borders --%>
                      <td style="background:#eee;">&nbsp;</td>
                    </c:otherwise>
                  </c:choose>
                </c:forEach>
              </tr>
            </c:forEach>
          </c:if>
        </table>

        <%--  The Summary table --%>
        <div id="summary" style="display:none;" >
            <div align="right" >
              <img style="padding-bottom: 4px"
                   onclick="javascript:Effect.Fade('summary', { duration: 0.30 });"
                   src="images/close.png" title="Close" 
                   onmouseout="this.style.cursor='normal';" 
                   onmouseover="this.style.cursor='pointer';"/>
            </div>
            <div id="summary_loading">Loading...</div>
            <div id="summary_loaded" style="display:none;"></div>
        </div>  
        <c:if test="${resultsTable.estimatedSize > 1}">
           <tiles:insert name="paging.tile">
             <tiles:put name="resultsTable" beanName="resultsTable" />
             <tiles:put name="currentPage" value="results" />
           </tiles:insert>
        </c:if>
</div>
        <%-- Return to main results link
             <c:if test="${!isWebResults
                         && QUERY_RESULTS != null && !fn:startsWith(param.table, 'bag')}">
               <p>
                 <html:link action="/results?table=results">
                   <fmt:message key="results.return"/>
                 </html:link>
               </p>
             </c:if>
             --%>

      </div> <%-- end of main results table body div --%>
    </html:form>

  </c:otherwise>
</c:choose>
<!-- /table.jsp -->
