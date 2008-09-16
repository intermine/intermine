<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1" prefix="str" %>

<!-- bagDetails.jsp -->
<html:xhtml/>
<link rel="stylesheet" href="css/resultstables.css" type="text/css" />

<script type="text/javascript">
<!--//<![CDATA[
  var modifyDetailsURL = '<html:rewrite action="/modifyDetails"/>';
  var detailsType = 'bag';
//]]>-->
</script>
<script type="text/javascript" src="js/inlinetemplate.js">
  var modifyDetailsURL = '<html:rewrite action="/modifyDetails"/>';
</script>
<script type="text/javascript" src="js/widget.js"></script>

<div class="heading">
     <fmt:message key="bagDetails.title"/> <span style="font-size:0.9em;font-weight:normal">for <b>${bag.name}</b> (${bag.size} ${bag.type}s)</span>
</div>

<div class="body">

<table cellspacing="0" width="100%">
<tr>
  <TD colspan=2 align="left" style="padding-bottom:10px">
<link rel="stylesheet" href="css/toolbar.css" type="text/css" media="screen" title="Toolbar Style" charset="utf-8"/>
<script type="text/javascript" src="js/toolbar.js"></script>
<div id="tool_bar_div">
    <ul id="button_bar" onclick="toggleToolBarMenu(event);">
        <li id="tool_bar_li_display"><img style="cursor: pointer;" src="images/icons/null.gif" width="62" height="25" title="Display related templates or widgets" border="0" id="tool_bar_button_display" class="tool_bar_button"/></li>
        <li id="tool_bar_li_export"><img style="cursor: pointer;" src="images/icons/null.gif" width="64" height="25" title="Export this list" border="0" id="tool_bar_button_export" class="tool_bar_button"/></li>
        <li id="tool_bar_li_use"><img style="cursor: pointer;" src="images/icons/null.gif" width="43" height="25" title="Use this list in a template or a query" border="0" id="tool_bar_button_use" class="tool_bar_button"/></li>
        <c:if test="${myBag == 'true'}">
          <li id="tool_bar_li_edit"><img style="cursor: pointer;" src="images/icons/null.gif" width="56" height="25" alt="Edit" border="0" id="tool_bar_button_edit" class="tool_bar_button"></li>
    </c:if>
        <li class="tool_bar_separator"><span>&nbsp;//&nbsp;</span></li>
        <li class="tool_bar_link">
           <html:form action="/findInList">
            <input type="text" name="textToFind" id="textToFind"/>
            <input type="hidden" name="bagName" value="${bag.name}"/>
            <html:submit>
              <fmt:message key="bagDetails.findInList"/>
            </html:submit>
          </html:form>
        </li>
    </ul>
</div>

<html:form action="/modifyBagDetailsAction" styleId="bagDetailsForm">
<html:hidden property="bagName" value="${bag.name}"/>

<div id="tool_bar_item_display" style="visibility:hidden;width:100px" class="tool_bar_item">
    <html:link anchor="relatedTemplates" action="bagDetails?bagName=${bag.name}">Related templates</html:link><br/>
    <html:link anchor="widgets" action="bagDetails?bagName=${bag.name}">Related widgets</html:link>
    <hr/>
    <a href="javascript:hideMenu('tool_bar_item_display')"><fmt:message key="confirm.cancel"/></a>
</div>

<div id="tool_bar_item_export" style="visibility:hidden;width:300px" class="tool_bar_item">
    <c:set var="tableName" value="bag.${bag.name}" scope="request"/>
    <c:set var="pagedTable" value="${pagedResults}" scope="request"/>
    <tiles:get name="export.tile"/>
    <hr>
    <a href="javascript:hideMenu('tool_bar_item_export')" ><fmt:message key="confirm.cancel"/></a>
</div>

<div id="tool_bar_item_use" style="visibility:hidden;width:100px" class="tool_bar_item">
    <html:link action="/modifyBagDetailsAction.do?useBag=1&bagName=${bag.name}">In a query</html:link><br/>
  <html:link action="/templates">In a template</html:link>
    <hr/>
    <a href="javascript:hideMenu('tool_bar_item_use')" ><fmt:message key="confirm.cancel"/></a>
</div>

<div id="tool_bar_item_edit" style="visibility:hidden;width:300px" class="tool_bar_item">
  <%-- add selected to bag --%>
  <fmt:message key="bagDetails.addRecords"/>:<br/>
   <c:choose>
   <c:when test="${!empty PROFILE.savedBags && fn:length(PROFILE.savedBags) > 1}">
          <html:select property="existingBagName">
             <c:forEach items="${PROFILE.savedBags}" var="entry">
              <c:if test="${param.bagName != entry.key}">
                <html:option value="${entry.key}">${entry.key} [${entry.value.type}]</html:option>
              </c:if>
             </c:forEach>
          </html:select>
     <input type="submit" name="addToBag" id="addToBag" value="Add" />
     <script type="text/javascript" charset="utf-8">
          $('addToBag').disabled = true;
        </script>
    </c:when>
    <c:otherwise>
      <em><fmt:message key="toolbar.noLists"/></em>
    </c:otherwise>
    </c:choose>
  <br/>
    <%-- remove selected from bag --%>
    <fmt:message key="bagDetails.deleteRecords"/>:<br>
    <input type="submit" name="removeFromBag" id="removeFromBag" value="Remove" disabled="true" />
    <hr>
  <a href="javascript:hideMenu('tool_bar_item_edit')" ><fmt:message key="confirm.cancel"/></a>
</div>

</TD>
</TR>
<TR>

<TD valign="top" class="tableleftcol">
<div>

<%-- Table displaying bag elements --%>
<tiles:insert name="resultsTable.tile">
     <tiles:put name="pagedResults" beanName="pagedResults" />
     <tiles:put name="currentPage" value="bagDetails" />
     <tiles:put name="bagName" value="${bag.name}" />
     <tiles:put name="highlightId" value="${highlightId}"/>
</tiles:insert>
</div>

<tiles:insert name="paging.tile">
  <tiles:put name="resultsTable" beanName="pagedResults" />
  <tiles:put name="currentPage" value="bagDetails" />
  <tiles:put name="bag" beanName="bag" />
</tiles:insert>

<div id="clearLine">&nbsp;</div>

<div style="clear:both">

<%-- Bag Description --%>
<c:choose>
    <c:when test="${myBag == 'true'}">
      <div id="bagDescriptionDiv" onclick="Element.toggle('bagDescriptionDiv');Element.toggle('bagDescriptionTextarea');$('textarea').focus()">
        <h3><img src="images/icons/description.png" title="Description of your list"/>&nbsp;Description</h3>
        <c:choose>
          <c:when test="${! empty bag.description}">
            <p><c:out value="${bag.description}" escapeXml="false" /></p>
          </c:when>
          <c:otherwise>
            <div id="emptyDesc"><fmt:message key="bagDetails.bagDescr"/></div>
          </c:otherwise>
        </c:choose>
      </div>
      <div id="bagDescriptionTextarea" style="display:none">
        <textarea id="textarea"><c:if test="${! empty bag.description}"><c:out value="${fn:replace(bag.description,'<br/>','')}" /></c:if></textarea>
        <div align="right">
          <button onclick="Element.toggle('bagDescriptionTextarea');
              Element.toggle('bagDescriptionDiv'); return false;"><fmt:message key="confirm.cancel"/></button>
          <button onclick="saveBagDescription('${bag.name}'); return false;"><fmt:message key="button.save"/></button>
        </div>
      </div>
      </c:when>
      <c:when test="${! empty bag.description}">
      <div id="bagDescriptionDiv">
          <b>Description:</b> ${bag.description}
      </div>
      </c:when>
</c:choose>

</div>

</TD>

<TD align="left" valign="top" width="40%">

<div id="listinfo" class="listtoolbox" align="left">
<h3><img src="images/icons/info.png" title="Information about this list"/>&nbsp;List Info</h3>
<p>
<table cellpadding="0" cellspacing="0" border="0" class="listinfotable">
<tr>
  <td class="infotitle">Name:</td><td>${bag.name}</td>
</tr><tr>
  <td class="infotitle">Date Created:</td><td><im:dateDisplay date="${bag.dateCreated}" /></td>
</tr><tr>
  <td class="infotitle">Type:</td><td>${bag.type}</td>
</tr><tr>
  <td class="infotitle">Size:</td><td>${bag.size} records</td>
</tr>
</table>
</p>
</div>

<div id="convertList" class="listtoolbox" align="left">
<h3><img src="images/icons/convert.png" title="Convert objects in this bag to different type"/>&nbsp;Convert</h3>
<p>


<tiles:insert name="convertBag.tile">
     <tiles:put name="bag" beanName="bag" />
     <tiles:put name="idname" value="cp" />
     <tiles:put name="orientation" value="h" />
</tiles:insert>

</p>
</div>
</html:form>
<tiles:insert page="/bagDisplayers.jsp">
   <tiles:put name="bag" beanName="bag"/>
</tiles:insert>

</TD></TR>
</TABLE>

<div class="heading" style="clear:both;margin-top:15px">
     <a id="widgets">Widgets displaying properties of '${bag.name}'</a> &nbsp;
</div>
<script language="javascript">
  function toggleWidget(widgetid,linkid) {
    Element.toggle($(widgetid));
    var d = $(linkid);
    if(Element.hasClassName($(linkid), 'active')) {
      $(linkid).removeClassName('active');
      AjaxServices.saveToggleState(widgetid, false);
    } else {
      $(linkid).addClassName('active');
      AjaxServices.saveToggleState(widgetid, true);
    }
  }
</script>

<ol class="widgetList">
<li>Click to select widgets you would like to display:</li>
<c:forEach items="${widgets}" var="widget">
  <li><a href="javascript:toggleWidget('widgetcontainer${widget.id}','togglelink${widget.id}')" id="togglelink${widget.id}" class="active">${widget.title}</a>&nbsp;|&nbsp;</li>
</c:forEach>
</ol>
<div style="clear:both;">&nbsp;</div>

<link rel="stylesheet" type="text/css" href="<html:rewrite page='/css/widget.css'/>"/>
<c:forEach items="${widgets}" var="widget">
  <tiles:insert name="widget.tile">
    <tiles:put name="widget" beanName="widget"/>
    <tiles:put name="bag" beanName="bag"/>
    <tiles:put name="widget2extraAttrs" beanName="widget2extraAttrs" />
  </tiles:insert>
</c:forEach>
<div style="clear:both;">&nbsp;</div>

<!-- templates -->

<c:set var="templateIdPrefix" value="bagDetailsTemplate${bag.type}"/>
<c:set value="${fn:length(CATEGORIES)}" var="aspectCount"/>
<div class="heading">
   <a id="relatedTemplates">Template results for '${bag.name}' &nbsp;</a>&nbsp;&nbsp;<span style="font-size:0.8em;">
  (<a href="javascript:toggleAll(${aspectCount}, '${templateIdPrefix}', 'expand', null, true);">expand all <img src="images/disclosed.gif"/></a> / <a href="javascript:toggleAll(${aspectCount}, '${templateIdPrefix}', 'collapse', null, true);">collapse all <img src="images/undisclosed.gif"/></a>)</span>
  </div>


  <div class="body">
  <fmt:message key="bagDetails.templatesHelp">
    <fmt:param>
              <img src="images/disclosed.gif"/> / <img src="images/undisclosed.gif"/>
      </fmt:param>
  </fmt:message>

  <%-- Each aspect --%>
  <c:forEach items="${CATEGORIES}" var="aspect" varStatus="status">
    <tiles:insert name="objectDetailsAspect.tile">
      <tiles:put name="placement" value="aspect:${aspect}"/>
      <tiles:put name="trail" value="|bag.${bag.name}"/>
      <tiles:put name="interMineIdBag" beanName="bag"/>
      <tiles:put name="aspectId" value="${templateIdPrefix}${status.index}" />
      <tiles:put name="opened" value="${status.index == 0}" />
    </tiles:insert>
  </c:forEach>

</div>  <!-- templates body -->

</div>  <!-- whole page body -->

<!-- /templates -->
<!-- /bagDetails.jsp -->
