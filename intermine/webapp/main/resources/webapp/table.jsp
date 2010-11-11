<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
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
<link rel="stylesheet" href="css/table.css" type="text/css" />
<tiles:get name="objectTrail.tile"/> <%--<im:vspacer height="1"/>--%>

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

    <div class="body">
      <div class="resultsTableTemplateHeader">

      <c:if test="${!empty templateQuery || !empty param.templateQueryTitle}">

      <%-- show the description only if we ve run a query (rather than viewing
           a bag) - see #1031 --%>
        <c:if test="${empty param.bagName}">
          <div>
            <fmt:message key="results.templateTitle"/>:
            <span class="templateTitleBold">
            <c:choose>
              <c:when test="${!empty param.templateQueryTitle}">
                <c:out value="${param.templateQueryTitle}"/>
              </c:when>
              <c:otherwise>
                <c:out value="${templateQuery.title}"/>
              </c:otherwise>
            </c:choose>
            </span>
          </div>
       </c:if>
     </c:if>

     <c:if test="${!empty pathQuery.description || !empty param.templateQueryDescription}">
       <div class="templateDescription">
         <c:choose>
           <c:when test="${!empty param.templateQueryDescription}">
             <c:out value="${param.templateQueryDescription}"/>
           </c:when>
           <c:otherwise>
             <c:out value="${pathQuery.description}"/>
           </c:otherwise>
         </c:choose>
         </div>
     </c:if>

     <c:if test="${!empty param.bagName}">
       <div><strong id="numberOfResults">${resultsTable.estimatedSize}</strong> results for list:  <c:out value="${param.bagName}"/></div>
     </c:if>

       </div>
     </div>


<c:choose>
  <c:when test="${resultsTable.estimatedSize == 0}">
    <div class="altmessage">
      <fmt:message key="results.pageinfo.empty"/><br/>
    </div>
  </c:when>
  <c:otherwise>


<div class="body">

<%--
<c:if test="${! empty GALAXY_URL}">
  <tiles:insert name="galaxy.tile"/>
</c:if>
--%>

<%-- Toolbar --%>
<link rel="stylesheet" href="css/toolbar.css" type="text/css" />
<link rel="stylesheet" href="css/tablePageLinks.css" type="text/css" >
<script type="text/javascript" src="js/toolbar.js"></script>
<script type="text/javascript" charset="utf-8">
    jQuery(document).ready(function () {
        jQuery(".tb_button").click(function () {
            toggleToolBarMenu(this);
        });
    })
</script>
<div id="tool_bar_div">
<ul id="button_bar">
    <li id="tool_bar_li_createlist" class="tb_button"><img src="images/icons/lists-16.png" width="16" height="16" alt="Create"><html:link linkName="#">Create List</html:link></li>
    <li id="tool_bar_li_addtolist" class="tb_button"><img src="images/add.png" width="15" height="13" alt="Add"><html:link linkName="#">Add to List</html:link></li>
    <li id="tool_bar_li_addcolumn" class="tb_button"><img src="images/addcol.png" width="9" height="13" alt="Addcol"><html:link linkName="#">Add Column</html:link></li>
    <li id="tool_bar_li_export" class="tb_button"><img src="images/export.png" width="12" height="13" alt="Export"><html:link linkName="#">Export</html:link></li>

    <li class="tool_bar_link" style="padding:2px">
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
<li class="tool_bar_link">
  <tiles:insert page="/tablePageLinks.jsp">
    <tiles:put name="short" value="false" />
     <tiles:put name="currentPage" value="results" />
   </tiles:insert>
</li>
</ul>
</div>

<%-- Create new list --%>
<html:form action="/saveBag" >
<input type="hidden" name="operationButton"/>

<div id="tool_bar_item_createlist" style="display:none;width:350px" class="tool_bar_item" >
      <em>(with selected items)</em>
<%-- FIXME: selectedIds has gone, we need a new plan:   $ { pagedResults.selectedIds.length} --%>
      <fmt:message key="bag.new"/><br/>
      <input type="text" name="newBagName" id="newBagName" onkeypress="if (event.keyCode == 13) {validateBagName('saveBagForm');return false;}"/>
      <input type="hidden" name="__intermine_forward_params__" value="${pageContext.request.queryString}"/>
      <input type="hidden" name="table" value="${param.table}"/>
      <input type="button" name="saveNewBag" value="Save selected" id="saveNewBag" onclick="javascript:validateBagName('saveBagForm');"/>
      <script type="text/javascript" charset="utf-8">
        jQuery('#newBagName').attr('disabled','disabled');
        jQuery('#saveNewBag').attr('disabled','disabled');
      </script>
    <hr>
  <a href="javascript:hideMenu('tool_bar_item_createlist')" ><fmt:message key="confirm.cancel"/></a>
</div>
<%-- Add to existing list --%>
<div id="tool_bar_item_addtolist" style="display:none;width:300px" class="tool_bar_item" >
   <c:choose>
   <c:when test="${!empty PROFILE.savedBags}">
          <fmt:message key="bag.existing"/>
          <html:select property="existingBagName">
             <c:forEach items="${PROFILE.savedBags}" var="entry">
              <c:if test="${param.bagName != entry.key}">
                <html:option value="${entry.key}">${entry.key} [${entry.value.type}]</html:option>
              </c:if>
             </c:forEach>
          </html:select>
     <input type="submit" name="addToBag" id="addToBag" value="Add selected" />
     <script type="text/javascript" charset="utf-8">
          jQuery('#addToBag').attr('disabled','disabled');
        </script>
    </c:when>
    <c:otherwise>
      <em><fmt:message key="toolbar.noLists"/></em>
    </c:otherwise>
    </c:choose>
    <hr>
  <a href="javascript:hideMenu('tool_bar_item_addtolist')" >Cancel</a>
</div>

<%-- Export --%>
<div id="tool_bar_item_export" style="display:none;width:370px" class="tool_bar_item">
    <c:set var="tableName" value="${param.table}" scope="request"/>
    <c:set var="pagedTable" value="${resultsTable}" scope="request"/> <!-- This is not used by ExportController-->
    <tiles:get name="export.tile"/>
    <hr>
  <a href="javascript:hideMenu('tool_bar_item_export')" ><fmt:message key="confirm.cancel"/></a>
</div>

<div class="resultsPage">
<tiles:insert name="resultsTable.tile">
     <tiles:put name="pagedResults" beanName="resultsTable" />
     <tiles:put name="currentPage" value="results" />
</tiles:insert>
</div>

      </div> <%-- end of main results table body div --%>
    </html:form>

<div id="tool_bar_item_addcolumn" style="display:none;font-size:0.7em" class="tool_bar_item">
    <tiles:insert name="addColumn.tile">
      <tiles:put name="table" value="${param.table}" />
      <tiles:put name="trail" value="${param.trail}" />
    </tiles:insert>
    <hr>
    <a href="javascript:hideMenu('tool_bar_item_addcolumn')" ><fmt:message key="confirm.cancel"/></a>
</div>


  </c:otherwise>
</c:choose>

<script type="text/javascript">
	// exists function
	jQuery.fn.exists = function(){ return jQuery(this).length>0; }
	// set the actual number of results on top of the table
	jQuery(document).ready(function() {
		if (document.resultsCountText && jQuery("strong#numberOfResults").exists()) {
			// get only the digits
			jQuery("strong#numberOfResults").text(document.resultsCountText.replace(/[^\d]/g, ""));
		}
	});
</script>

<!-- /table.jsp -->
