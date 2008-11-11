<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<html:xhtml/>

<tiles:importAttribute name="pagedResults" ignore="false"/>
<tiles:importAttribute name="currentPage" ignore="false"/>
<tiles:importAttribute name="bagName" ignore="true"/>
<tiles:importAttribute name="highlightId" ignore="true"/>

<script type="text/javascript" src="js/table.js" ></script>
<link rel="stylesheet" href="css/resultstables.css" type="text/css" />
<script type="text/javascript">
<!--//<![CDATA[
  function changePageSize() {
    var url = '${requestScope['javax.servlet.include.context_path']}/results.do?';
    var pagesize = document.changeTableSizeForm.pageSize.options[document.changeTableSizeForm.pageSize.selectedIndex].value;
    var page = ${pagedResults.startRow}/pagesize;
    url += 'table=${param.table}' + '&page=' + Math.floor(page) + '&size=' + pagesize;
    if ('${param.trail}' != '') {
        url += '&trail=${param.trail}';
    }
    document.location.href=url;
  }
  /*var columnsToDisable = ${columnsToDisable};
  var columnsToHighlight = ${columnsToHighlight};
  var bagType = null;*/

jQuery(document).ready(function(){
    jQuery('.th_drag').draggable({
      revert: true, 
      helper: "clone",
      opacity: 0.40
    });
    jQuery('.th_drop').droppable({ 
      accept: '.th_drag', 
      hoverClass: 'droppable-col-hover', 
      drop: function(ev, ui) {
        var index1=jQuery(ui.draggable).attr('id');
        var index2=jQuery(this).attr('id');
        if(index1 != index2) {
          top.location='<html:rewrite action="/changeTable.do?currentPage=${currentPage}&bagName=${bagName}&table=${param.table}&method=swapColumns&trail=${param.trail}"/>&index1='+index1+'&index2='+index2;
        }
      } 
    });
});  

//]]>-->
</script>

<c:set var="colcount" value="0" />
<%--<html:form action="/saveBag" >--%>
<table class="results" cellspacing="0">

  <%-- The headers --%>
  <thead>
  <tr>
    <c:forEach var="column" items="${pagedResults.columns}" varStatus="status">
      <c:set var="colcount" value="${colcount+1}"/>
      <im:formatColumnName outVar="displayPath" str="${column.name}" />
      <im:unqualify className="${column.name}" var="pathEnd"/>
      <im:prefixSubstring str="${column.name}" outVar="columnPathPrefix" delimiter="."/>
      <c:choose>
        <c:when test="${!empty QUERY
                      && !empty QUERY.pathStringDescriptions[columnPathPrefix] && empty notUseQuery}">
          <c:set var="columnDisplayName"
                 value="<span class='viewPathDescription' title='${displayPath}'>${QUERY.pathStringDescriptions[columnPathPrefix]}</span> &gt; ${pathEnd}"/>
        </c:when>
        <c:otherwise>
          <c:set var="columnDisplayName" value="${displayPath}"/>
        </c:otherwise>
      </c:choose>

      <c:choose>
        <c:when test="${column.visible}">
          <th align="center" valign="top" >
            <div id="${column.index}" class="th_drop">
             <%--<c:if test="${not empty sortOrderMap[column.name] && empty bag}">
                  <img border="0"
                       width="17" height="16" src="images/${sortOrderMap[column.name]}_gray.gif"
                          title="Results are sorted by ${column.name}"/>
             </c:if>--%>
            <div id="${column.index}" class="th_drag">
          
          <c:if test="${column.selectable}">
            <%--<c:set var="colcount" value="${colcount+1}"/>
              <th align="center" class="checkbox">--%>
              <c:set var="disabled" value="false"/>
              <c:if test="${(!empty resultsTable.selectedClass) && (resultsTable.selectedClass != column.typeClsString)}">
                <c:set var="disabled" value="true"/>
              </c:if>
              <html:multibox property="currentSelectedIdStrings" name="pagedResults" styleId="selectedObjects_${status.index}"
                             styleClass="selectable"
                             onclick="selectAll(${status.index}, '${column.typeClsString}','${pagedResults.tableid}')"
                             disabled="${disabled}">
                <c:out value="${column.columnId}"/>
              </html:multibox>
            <%--</th>--%>
          </c:if>
              <im:abbreviate value="${columnDisplayName}" length="20"/>
              <im:typehelp type="${column.path}" fullPath="true"/>
              <%-- summary --%>
              <c:if test="${!empty column.path.noConstraintsString}">
                <fmt:message key="columnsummary.getsummary" var="summaryTitle" />
                <a href="javascript:getColumnSummary('${pagedResults.tableid}','${column.path.noConstraintsString}', &quot;${columnDisplayName}&quot;)"
                   title="${summaryTitle}"><img src="images/summary_maths.png" title="${summaryTitle}"/></a>
              </c:if>
            </div>
            </div>
          </th>
        </c:when>
        <c:otherwise>
          <th>
            <%-- <fmt:message key="results.showColumnHelp" var="showColumnTitle">
                 <fmt:param value="${column.name}"/>
                 </fmt:message> --%>
            <html:link action="/changeTable?currentPage=${currentPage}&amp;bagName=${bagName}&amp;table=${param.table}&amp;method=showColumn&amp;index=${status.index}&amp;trail=${param.trail}"
                       title="${showColumnTitle}">
              <img src="images/show-column.gif" title="${fn:replace(column.name, '.', '&nbsp;> ')}"/>
            </html:link>
          </th>
        </c:otherwise>
      </c:choose>
    </c:forEach>
  </tr>
  </thead>
  <%-- The data --%>

  <%-- Row --%>
  <c:if test="${pagedResults.estimatedSize > 0}">
  <tbody>
    <c:forEach var="row" items="${pagedResults.resultElementRows}" varStatus="status">

      <c:set var="rowClass">
        <c:choose>
          <c:when test="${status.count % 2 == 1}">odd</c:when>
          <c:otherwise>even</c:otherwise>
        </c:choose>
      </c:set>

      <im:instanceof instanceofObject="${row}" instanceofClass="org.intermine.objectstore.flatouterjoins.MultiRow" instanceofVariable="isMultiRow"/>
      <c:choose>
        <c:when test="${isMultiRow == 'true'}">
          <c:forEach var="subRow" items="${row}" varStatus="multiRowStatus">
            <tr class="<c:out value="${rowClass}"/>">
              <c:forEach var="column" items="${pagedResults.columns}" varStatus="status2">

                <c:choose>
                  <c:when test="${column.visible}">
                    <im:instanceof instanceofObject="${subRow[column.index]}" instanceofClass="org.intermine.objectstore.flatouterjoins.MultiRowFirstValue" instanceofVariable="isFirstValue"/>
                    <c:if test="${isFirstValue == 'true'}">
                      <c:set var="resultElement" value="${subRow[column.index].value}" scope="request"/>
                      <c:set var="highlightObjectClass" value="noHighlightObject"/>
                      <c:if test="${!empty highlightId && resultElement.id == highlightId}">
                        <c:set var="highlightObjectClass" value="highlightObject"/>
                      </c:if>

                      <%-- test whether already selected and highlight if needed --%>
                      <c:set var="cellClass" value="${resultElement.id}"/>
                      <c:if test="${resultElement.selected && empty bagName}">
                        <c:set var="cellClass" value="${cellClass} highlightCell"/>
                      </c:if>

                      <td id="cell,${status2.index},${status.index},${subRow[column.index].value.typeClsString}"
                       class="${highlightObjectClass} id_${resultElement.id} class_${subRow[column.index].value.typeClsString} ${ischecked}" rowspan="${subRow[column.index].rowspan}">
                      <%-- the checkbox to select this object --%>
                      <c:if test="${column.selectable}">
                        <c:set var="checkboxClass" value="checkbox ${resultElement.id}"/>
                        <c:if test="${resultElement.selected}">
                          <c:set var="checkboxClass" value="${checkboxClass} highlightCell"/>
                        </c:if>
                        <%--<td align="center" class="checkbox ${highlightObjectClass} id_${resultElement.id} class_${subRow[column.index].value.typeClsString} ${ischecked}" id="cell_checkbox,${status2.index},${(status.index + 1) * 1000 + multiRowStatus.index},${subRow[column.index].value.typeClsString}" rowspan="${subRow[column.index].rowspan}">--%>
                          <c:if test="${resultElement.id != null}">
                            <html:multibox property="currentSelectedIdStrings" name="pagedResults"
                                 styleId="selectedObjects_${status2.index}_${status.index}_${subRow[column.index].value.typeClsString}"
                                 styleClass="selectable id_${resultElement.id} index_${column.index} class_${subRow[column.index].value.typeClsString} class_${column.typeClsString}"
                                 onclick="itemChecked(${status.index},${status2.index}, '${pagedResults.tableid}', this)"
                                 disabled="${disabled}">
                              <c:out value="${resultElement.id}"/>
                            </html:multibox>
                          </c:if>
                        <%--</td>--%>
                      </c:if>
                        <c:set var="columnType" value="${column.type}" scope="request"/>
                          <tiles:insert name="objectView.tile" /> <%-- uses resultElement? --%>
                      </td>
                    </c:if>
                  </c:when>
                  <c:otherwise>
                    <%-- add a space so that IE renders the borders --%>
                    <td style="background:#eee;">&nbsp;</td>
                  </c:otherwise>
                </c:choose>
              </c:forEach>
            </tr>
          </c:forEach>
        </c:when>
        <c:otherwise>
          <tr class="<c:out value="${rowClass}"/>">
            <c:forEach var="column" items="${pagedResults.columns}" varStatus="status2">

              <c:choose>
                <c:when test="${column.visible}">
                  <c:set var="resultElement" value="${row[column.index]}" scope="request"/>
                  <c:set var="highlightObjectClass" value="noHighlightObject"/>
                  <c:if test="${!empty highlightId && resultElement.id == highlightId}">
                    <c:set var="highlightObjectClass" value="highlightObject"/>
                  </c:if>

                  <%-- the checkbox to select this object --%>
                  <c:set var="ischecked" value=""/>
                    <fmt:formatNumber value="${resultElement.id}" var="resultElementIdString" scope="page" />
                    <c:forEach items="${pagedResults.currentSelectedIdStrings}" var="selectedId">
                      <c:if test="${(! empty resultElement.typeClsString) && (fn:replace(resultElementIdString,',','') == selectedId)}">
                        <c:set var="ischecked" value="highlightCell"/>
                      </c:if>
                    </c:forEach>
                  <c:set var="disabled" value="false"/>
                  <c:if test="${(!empty resultsTable.selectedClass) && ((resultsTable.selectedClass != resultElement.typeClsString)&&(resultsTable.selectedClass != column.typeClsString) && resultsTable.selectedColumn != column.index)}">
                    <c:set var="disabled" value="true"/>
                  </c:if>
                  <%-- test whether already selected and highlight if needed --%>
                  <td id="cell,${status2.index},${status.index},${row[column.index].typeClsString}"
                       class="${highlightObjectClass} id_${resultElement.id} class_${row[column.index].typeClsString} ${ischecked}">
                    <c:set var="columnType" value="${column.type}" scope="request"/>
                    <div>
                  <c:if test="${column.selectable}">
                    <%--<td align="center" class="checkbox ${highlightObjectClass} id_${resultElement.id} class_${row[column.index].typeClsString} ${ischecked}" id="cell_checkbox,${status2.index},${status.index},${row[column.index].typeClsString}">--%>
                      <c:if test="${resultElement.id != null}">
                        <html:multibox property="currentSelectedIdStrings" name="pagedResults"
                                 styleId="selectedObjects_${status2.index}_${status.index}_${row[column.index].typeClsString}"
                                 styleClass="selectable id_${resultElement.id} index_${column.index} class_${row[column.index].typeClsString} class_${column.typeClsString}"
                                 onclick="itemChecked(${status.index},${status2.index}, '${pagedResults.tableid}', this)"
                                 disabled="${disabled}">
                          <c:out value="${resultElement.id}"/>
                        </html:multibox>
                      </c:if>
                    <%--</td>--%>
                  </c:if>
                       <tiles:insert name="objectView.tile"/>
                    </div>
                  </td>
                </c:when>
                <c:otherwise>
                  <%-- add a space so that IE renders the borders --%>
                  <td style="background:#eee;">&nbsp;</td>
                </c:otherwise>
              </c:choose>
            </c:forEach>
          </tr>
        </c:otherwise>
      </c:choose>
    </c:forEach>
    </tbody>
  </c:if>

  <tfoot>
  <tr>
  <td colspan="${colcount}">
  <html:hidden property="tableid" value="${pagedResults.tableid}" />
  <c:set var="selectedIds">
     <c:forEach items="${pagedResults.currentSelectedIdStrings}" var="selected" varStatus="status"><c:if test="${status.count > 1}">${selectedIds}, </c:if><c:out value="${selected}"/></c:forEach>
  </c:set>
  <c:set var="selectedIdFields">
     <c:forEach items="${firstSelectedFields}" var="selected" varStatus="status"><c:if test="${status.count > 1}">${selectedIdFields}, </c:if><c:out value="${selected}"/></c:forEach>
  </c:set>
  <b>Selected:</b><span id="selectedIdFields">
  <c:choose>
   <c:when test="${pagedResults.allSelected}">All selected on all pages</c:when>
   <c:otherwise>${selectedIdFields}</c:otherwise>
  </c:choose>
  </span>
  </td>
  </tr>
  </tfoot>
    <c:if test="${! pagedResults.emptySelection}">
    <script type="text/javascript" charset="utf-8">
    if ($('newBagName')) {
        $('newBagName').disabled = false;
  }
  if ($('saveNewBag')) {
    $('saveNewBag').disabled = false;
  }
    if ($('addToBag')) {
        $('addToBag').disabled = false;
    }
    if ($('removeFromBag')) {
        $('removeFromBag').disabled = false;
    }
    </script>
    </c:if>

</table>
<%--  The Summary table --%>
<div id="summary" >
    <div align="right" id="handle">
      <img style="float:right";
           src="images/close.png" title="Close"
           onclick="javascript:jQuery('#summary').hide(300);"
           onmouseout="this.style.cursor='normal';"
           onmouseover="this.style.cursor='pointer';"/>
    </div>
    <div id="summary_loading"><img src="images/wait18.gif" title="loading icon">&nbsp;Loading...</div>
    <div id="summary_loaded" style="display:none;"></div>
</div>
<script language="javascript">
  <!--//<![CDATA[
  jQuery(document).ready(function(){
   jQuery('#summary').draggable({handle:'#handle'});
  });
   //new Draggable('summary',{handle:'handle'});
  //]]>-->
</script>

<c:if test="${empty bagName}">
   <div style="margin-top: 10px;">
   <tiles:insert name="paging.tile">
     <tiles:put name="resultsTable" beanName="resultsTable" />
     <tiles:put name="currentPage" value="results" />
   </tiles:insert>
   </div>
</c:if>
<%--</html:form>--%>
