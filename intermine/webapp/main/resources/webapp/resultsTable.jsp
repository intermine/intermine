<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
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
<tiles:importAttribute name="inlineTable" ignore="true"/>
<tiles:importAttribute name="highlightId" ignore="true"/>

<script type="text/javascript">
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
</script>

<c:set var="colcount" value="0" />
<table class="results" cellspacing="0" width="100%">

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
                      && !empty QUERY.descriptions[columnPathPrefix] && empty notUseQuery}">
          <c:set var="columnDisplayName"
                 value="<span class='viewPathDescription' title='${displayPath}'>${QUERY.descriptions[columnPathPrefix]}</span> &gt; ${pathEnd}"/>
        </c:when>
        <c:otherwise>
          <c:set var="columnDisplayName" value="${displayPath}"/>
        </c:otherwise>
      </c:choose>

      <th align="center" class="columnHeader">
        <%-- summary --%>
        <c:if test="${!empty column.path.noConstraintsString && empty inlineTable}">
          <fmt:message key="columnsummary.getsummary" var="summaryTitle" />
          <a href="javascript:getColumnSummary('${pagedResults.tableid}','${column.path.noConstraintsString}', &quot;${columnDisplayName}&quot;)"
               title="${summaryTitle}" class="summary_link"><img src="images/summary_maths.png" title="${summaryTitle}"/></a>
        </c:if>
        <!-- <div class="column-header-content"> -->
            <table border="0" cellspacing="0" cellpadding="0" class="column-header-content">
                <tr>
            <c:if test="${column.selectable && empty inlineTable}">
              <c:set var="disabled" value="false"/>
              <c:if test="${(!empty pagedResults.selectedClass) && (pagedResults.selectedClass != column.typeClsString)}">
                <c:set var="disabled" value="true"/>
              </c:if>
              <td><html:multibox property="currentSelectedIdStrings" name="pagedResults" styleId="selectedObjects_${status.index}"
                             styleClass="selectable"
                             onclick="selectAll(${status.index}, '${column.typeClsString}','${pagedResults.tableid}')"
                             disabled="${disabled}">
                <c:out value="${column.columnId}"/>
              </html:multibox></td>
            </c:if>
            <td>
            <!-- Display actual column name -->
            <c:set var="columnDisplayNameList" value="${fn:split(column.name,'>')}"/>
            <c:set var="begin" value="0"/>
            <c:if test="${fn:length(columnDisplayNameList) > 3}">...
                <c:set var="begin" value="${fn:length(columnDisplayNameList)-3}"/>
            </c:if>
            <span id="header_${fn:replace(pagedResults.tableid,'.','_')}_${status.count}" style="cursor:default;">
            <script type="text/javascript" charset="utf-8">
                jQuery(document).ready(function(){
                    jQuery('#header_${fn:replace(pagedResults.tableid,'.','_')}_${status.count}').qtip({
                       content: '${displayPath}',
                       show: 'mouseover',
                       hide: 'mouseout',
                       position: {
                           corner: {
                              target: 'topLeft',
                              tooltip: 'bottomLeft'
                           }
                       },
                       style: {
                          tip: 'bottomLeft',
                          fontSize: '12px',
                          name: 'cream',
                          whiteSpace: 'nowrap'
                       }
                    });
                });
            </script>
            <em style="font-size:9px;">
            <c:forEach items="${columnDisplayNameList}" var="columnNameItem" varStatus="status2" begin="${begin}">
              <c:choose>
                <c:when test="${status2.last}">
                    </em><br/>${columnNameItem}
                    <c:set var="fieldName" value="${columnNameItem}"/>
                </c:when>
                <c:otherwise>
                    ${columnNameItem} &gt;
              </c:otherwise>
              </c:choose>
            </c:forEach>
            <!-- ${fn:length(columnDisplayNameList)}:${columnDisplayName} -->
            <im:typehelp type="${column.path}" fullPath="true"/>
            </span>
            </td></tr></table>
        <!-- </div> -->
      </th>
    </c:forEach>
  </tr>
  </thead>

  <%-- The data --%>

  <%-- Row --%>
  <c:if test="${pagedResults.estimatedSize > 0}">
  <tbody>
    <c:forEach var="row" items="${pagedResults.rows}" varStatus="status">

      <c:set var="rowClass">
        <c:choose>
          <c:when test="${status.count % 2 == 1}">odd</c:when>
          <c:otherwise>even</c:otherwise>
        </c:choose>
      </c:set>

      <c:forEach var="subRow" items="${row}" varStatus="multiRowStatus">
        <tr class="<c:out value="${rowClass}"/>">

        <%-- If a whole column is selected, find the ResultElement.id from the selected column, other columns with the same ResultElement.id may also need to be highlighted --%>
        <c:if test="${pagedResults.allSelected != -1}">
          <c:set var="selectedColumnResultElementId" value="${subRow[pagedResults.allSelected].value.id}"/>
        </c:if>

        <c:forEach var="column" items="${pagedResults.columns}" varStatus="status2">

          <c:choose>
            <c:when test="${column.visible}">
              <im:instanceof instanceofObject="${subRow[column.index]}" instanceofClass="org.intermine.api.results.flatouterjoins.MultiRowFirstValue" instanceofVariable="isFirstValue"/>
                <c:if test="${isFirstValue == 'true'}">
                  <c:set var="resultElement" value="${subRow[column.index].value}" scope="request"/>
                  <c:set var="highlightObjectClass" value="noHighlightObject"/>
                  <c:if test="${!empty highlightId && resultElement.id == highlightId}">
                    <c:set var="highlightObjectClass" value="highlightObject"/>
                  </c:if>

                  <%-- test whether already selected and highlight if needed --%>
                  <c:set var="cellClass" value="${resultElement.id}"/>

                  <%-- highlight cell if selected or if whole column selected and element isnt de-selected --%>
                  <c:choose>
                    <c:when test="${pagedResults.allSelected == -1}">
                      <c:if test="${!empty pagedResults.selectionIds[resultElement.id] && pagedResults.allSelected == -1 && empty bagName}">
                        <c:set var="cellClass" value="${cellClass} highlightCell"/>
                      </c:if>
                    </c:when>
                    <c:otherwise>
                      <c:if test="${resultElement.id == selectedColumnResultElementId && empty pagedResults.selectionIds[resultElement.id] && empty bagName}">
                        <c:set var="cellClass" value="${cellClass} highlightCell"/>
                      </c:if>
                    </c:otherwise>
                  </c:choose>

                  <td id="cell,${status2.index},${status.index},${subRow[column.index].value.type}"
                      class="${highlightObjectClass} id_${resultElement.id} class_${subRow[column.index].value.type} ${cellClass}" rowspan="${subRow[column.index].rowspan}">
                    <%-- the checkbox to select this object --%>
                    <c:set var="disabled" value="false"/>
                    <c:if test="${(!empty pagedResults.selectedClass) && ((pagedResults.selectedClass != resultElement.type)&&(pagedResults.selectedClass != column.typeClsString) && pagedResults.selectedColumn != column.index)}">
                      <c:set var="disabled" value="true"/>
                    </c:if>
                    <c:if test="${column.selectable && empty inlineTable}">
                      <c:set var="checkboxClass" value="checkbox ${resultElement.id}"/>
                      <c:if test="${!empty pagedResults.selectionIds[resultElement.id] && pagedResults.allSelected == -1}">
                        <c:set var="checkboxClass" value="${checkboxClass} highlightCell"/>
                      </c:if>
                      <%--<td align="center" class="checkbox ${highlightObjectClass} id_${resultElement.id} class_${subRow[column.index].value.type} ${ischecked}" id="cell_checkbox,${status2.index},${(status.index + 1) * 1000 + multiRowStatus.index},${subRow[column.index].value.type}" rowspan="${subRow[column.index].rowspan}">--%>
                      <c:if test="${resultElement.id != null}">
                        <html:multibox property="currentSelectedIdStrings" name="pagedResults"
                           styleId="selectedObjects_${status2.index}_${status.index}_${subRow[column.index].value.type}"
                           styleClass="selectable id_${resultElement.id} index_${column.index} class_${subRow[column.index].value.type} class_${column.typeClsString}"
                           onclick="itemChecked(${status.index},${status2.index}, '${pagedResults.tableid}', this)"
                           disabled="${disabled}">
                          <c:out value="${resultElement.id}"/>
                        </html:multibox>
                      </c:if>
                    </c:if>
                    <c:set var="columnType" value="${column.typeClsString}" scope="request"/>
                    <tiles:insert name="objectView.tile" >
                        <tiles:put name="id" value="${resultElement.id}" />
                        <tiles:put name="fieldName" value="${fieldName}" />
                    </tiles:insert>
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
    </c:forEach>
    </tbody>
  </c:if>

  <tfoot>
  <tr>
  <td colspan="${colcount}">
  <html:hidden property="tableid" value="${pagedResults.tableid}" />
  <c:choose>
    <c:when test="${empty inlineTable}">
      <b>Selected:</b><span id="selectedIdFields">
      <c:choose>
       <c:when test="${pagedResults.allSelected != -1}">All selected on all pages</c:when>
       <c:otherwise>
         <c:set var="selectedIds">
           <c:forEach items="${pagedResults.currentSelectedIdStrings}" var="selected" varStatus="status"><c:if test="${status.count > 1}">${selectedIds}, </c:if><c:out value="${selected}"/></c:forEach>
         </c:set>
         <c:set var="selectedIdFields">
           <c:forEach items="${firstSelectedFields}" var="selected" varStatus="status"><c:if test="${status.count > 1}">${selectedIdFields}, </c:if><c:out value="${selected}"/></c:forEach>
         </c:set>
         ${selectedIdFields}</c:otherwise>
      </c:choose>
      </span>
    </c:when>
    <c:otherwise>
      <c:set var="numRows" value="${pagedResults.exactSize}"/>

      <c:choose>
        <c:when test="${pagedResults.pageSize >= numRows}">
          <c:choose>
            <c:when test="${numRows == 1}">
              <b>Showing all <span><c:out value="${numRows}"/></span> row.</b>
            </c:when>
            <c:otherwise>
              <b>Showing all <span><c:out value="${numRows}"/></span> rows.</b>
            </c:otherwise>
          </c:choose>
        </c:when>
        <c:otherwise>
          <b>Showing first <span><c:out value="${pagedResults.pageSize}"/></span> of <span><c:out value="${numRows}"/></span> rows.</b>
        </c:otherwise>
      </c:choose>
    </c:otherwise>
  </c:choose>

  </td>
  </tr>
  </tfoot>
    <c:if test="${! pagedResults.emptySelection}">
    <script type="text/javascript" charset="utf-8">
    if (jQuery('#newBagName')) {
        jQuery('#newBagName').attr('disabled','');
  }
  if (jQuery('#saveNewBag')) {
    jQuery('#saveNewBag').attr('disabled','');
  }
    if (jQuery('#addToBag')) {
        jQuery('#addToBag').attr('disabled','');
    }
    if (jQuery('#removeFromBag')) {
        jQuery('#removeFromBag').attr('disabled','');
    }
    </script>
    </c:if>

</table>

<c:if test="${empty bagName && empty inlineTable}">
   <div style="margin-top: 10px;">
   <tiles:insert name="paging.tile">
     <tiles:put name="resultsTable" beanName="pagedResults" />
     <tiles:put name="currentPage" value="results" />
   </tiles:insert>
   </div>
</c:if>
<%--</html:form>--%>
