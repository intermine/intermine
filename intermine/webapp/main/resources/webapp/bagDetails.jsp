<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
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
<link rel="stylesheet" href="css/toolbar.css" type="text/css" media="screen" title="Toolbar Style" charset="utf-8"/>

<script type="text/javascript">
<!--//<![CDATA[
  var modifyDetailsURL = '<html:rewrite action="/modifyDetails"/>';
  var detailsType = 'bag';
//]]>-->
</script>
<script type="text/javascript" src="<html:rewrite page='/js/inlinetemplate.js'/>"></script>
<script type="text/javascript" src="<html:rewrite page='/js/widget.js'/>"></script>
<div class="body">
<c:choose>
<c:when test="${!empty bag}">
<div class="heading results">
  <img src="images/icons/lists-64.png" alt="lists icon"/>
  <h1>
    <fmt:message key="bagDetails.title"/> <span style="font-size:0.9em;font-weight:normal">for <b>${bag.name}</b> (${bag.size} ${bag.type}s)</span>
  </h1>
  <div id="tool_bar_div">
      <ul id="button_bar">
          <li id="tool_bar_li_export"class="tb_button"><img src="images/export.png" width="13" height="13" alt="Export this list"><html:link linkName="#">Export</html:link></li>
          <c:if test="${myBag == 'true'}">
            <li id="tool_bar_li_edit"class="tb_button"><img src="images/edit.png" width="13" height="13" alt="Edit my list"><html:link linkName="#">Edit</html:link></li>
      </c:if>
      </ul>
    <html:form styleId="findInListForm" action="/findInList">
        <input type="text" name="textToFind" id="textToFind"/>
          <input type="hidden" name="bagName" value="${bag.name}"/>
          <html:submit>
            <fmt:message key="bagDetails.findInList"/>
          </html:submit>
    </html:form>
  </div>
  <div style="clear:both;"></div>
</div>

<table cellspacing="0" width="100%">
<tr>
  <TD colspan=2 align="left" style="padding-bottom:10px">

<script type="text/javascript" src="js/toolbar.js"></script>
<script type="text/javascript" charset="utf-8">
    jQuery(document).ready(function () {
        jQuery(".tb_button").click(function () {
            toggleToolBarMenu(this);
        });

      // textarea resizer
      javascript:jQuery('textarea#textarea').autoResize({
          // on resize:
          onResize : function() {
            javascript:jQuery(this).css({opacity:0.8});
          },
          // after resize:
          animateCallback : function() {
            javascript:jQuery(this).css({opacity:1});
          },
          // quite slow animation:
          animateDuration : 300,
          // more extra space:
          extraSpace : 10
      });
    });
</script>

<html:form action="/modifyBagDetailsAction" styleId="bagDetailsForm">
<html:hidden property="bagName" value="${bag.name}"/>

<div id="tool_bar_item_display" style="display:none;width:100px" class="tool_bar_item">
    <html:link anchor="relatedTemplates" action="bagDetails?bagName=${bag.name}">Related templates</html:link><br/>
    <html:link anchor="widgets" action="bagDetails?bagName=${bag.name}">Related widgets</html:link>
    <hr/>
    <a href="javascript:hideMenu('tool_bar_item_display')"><fmt:message key="confirm.cancel"/></a>
</div>

<div id="tool_bar_item_export" style="display:none;width:300px" class="tool_bar_item">
    <c:set var="tableName" value="bag.${bag.name}" scope="request"/>
    <c:set var="pagedTable" value="${pagedResults}" scope="request"/>
    <tiles:get name="export.tile"/>
    <hr>
    <a href="javascript:hideMenu('tool_bar_item_export')" ><fmt:message key="confirm.cancel"/></a>
</div>

<div id="tool_bar_item_use" style="display:none;width:100px" class="tool_bar_item">
    <html:link action="/modifyBagDetailsAction.do?useBag=1&bagName=${bag.name}">In a query</html:link><br/>
  <html:link action="/templates">In a template</html:link>
    <hr/>
    <a href="javascript:hideMenu('tool_bar_item_use')" ><fmt:message key="confirm.cancel"/></a>
</div>

<div id="tool_bar_item_edit" style="display:none;width:300px" class="tool_bar_item">
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
          jQuery('#addToBag').attr('disabled','disabled');
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

<table style="margin-top: 10px;">
  <tr>
    <td><tiles:insert name="paging.tile">
      <tiles:put name="resultsTable" beanName="pagedResults" />
      <tiles:put name="currentPage" value="bagDetails" />
      <tiles:put name="bag" beanName="bag" />
    </tiles:insert></td>
    <c:if test="${PROFILE.loggedIn}">
      <td><div id="listTags">
        <table>
          <tr>
            <td><b>Tags&nbsp;&nbsp;</b></td>
            <td>
              <c:set var="taggable" value="${bag}"/>
              <tiles:insert name="inlineTagEditor.tile">
                     <tiles:put name="taggable" beanName="taggable"/>
                     <tiles:put name="vertical" value="true"/>
                     <tiles:put name="show" value="true"/>
                 </tiles:insert>
             </td>
          </tr>
        </table>
        </div></td>
    </c:if>
  </tr>
</table>

<div id="clearLine">&nbsp;</div>

<div style="clear:both">

<%-- Bag Description --%>
<c:choose>
    <c:when test="${myBag == 'true'}">
      <div id="bagDescriptionDiv" onclick="jQuery('#bagDescriptionDiv').toggle();jQuery('#bagDescriptionTextarea').toggle();jQuery('#textarea').focus()">
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
          <input type="button" onclick="jQuery('#bagDescriptionTextarea').toggle();
              jQuery('#bagDescriptionDiv').toggle(); return false;" value='<fmt:message key="confirm.cancel"/>' />
          <input type="button" onclick="saveBagDescription('${bag.name}'); return false;" value='<fmt:message key="button.save"/>' />
        </div>
      </div>
      </c:when>
      <c:when test="${! empty bag.description}">
      <div id="bagDescriptionDiv">
          <b>Description:</b> ${bag.description}
      </div>
      </c:when>
</c:choose>
<small>Date Created:  <im:dateDisplay date="${bag.dateCreated}" /></small>
</div>

<%-- BagDisplayers on Left --%>
    <tiles:insert page="/bagDisplayers.jsp">
       <tiles:put name="bag" beanName="bag"/>
       <tiles:put name="showOnLeft" value="true"/>
    </tiles:insert>

</TD>

<TD align="left" valign="top" width="40%">


<!-- closing toolbar div -->

<div id="convertList" class="listtoolbox" align="left">
<h3><img src="images/icons/convert.png" title="Convert objects in this bag to different type"/>&nbsp;Convert</h3>
<p>


<tiles:insert name="convertBag.tile">
     <tiles:put name="bag" beanName="bag" />
     <tiles:put name="idname" value="cp" />
     <tiles:put name="orientation" value="h" />
</tiles:insert>
</html:form>
</p>

<%-- BagDisplayers --%>
    <tiles:insert page="/bagDisplayers.jsp">
       <tiles:put name="bag" beanName="bag"/>
       <tiles:put name="showOnLeft" value="false"/>
    </tiles:insert>

</div>


<!-- link outs -->
<div id="linkOuts" class="listtoolbox" align="left">
  <h3>Link outs</h3>
     <p>
  <tiles:insert name="attributeLinkDisplayer.tile">
       <tiles:put name="bag" beanName="bag" />
  </tiles:insert>
  </p>
</div>

</TD>
</TR>
</TABLE>

<div class="heading" style="clear:both;margin-top:15px">
     <a id="widgets">Widgets displaying properties of '${bag.name}'</a> &nbsp;
</div>
<script language="javascript">
  function toggleWidget(widgetid,linkid) {
    jQuery('#'+widgetid).toggle();
    if(jQuery('#'+linkid).hasClass('active')) {
      jQuery('#'+linkid).removeClass('active');
      AjaxServices.saveToggleState(widgetid, false);
    } else {
      jQuery('#'+linkid).addClass('active');
      AjaxServices.saveToggleState(widgetid, true);
    }
  }
</script>

<p id="toggleWidgets">Click to select widgets you would like to display:
  <ol class="widgetList">
  <c:forEach items="${widgets}" var="widget">
    <li><a title="toggle widget" href="javascript:toggleWidget('widgetcontainer${widget.id}','togglelink${widget.id}')" id="togglelink${widget.id}" class="active">${widget.title}</a></li>
  </c:forEach>
  </ol>
</p>
<div style="clear:both;"></div>

<link rel="stylesheet" type="text/css" href="<html:rewrite page='/css/widget.css'/>"/>
<c:forEach items="${widgets}" var="widget">
  <tiles:insert name="widget.tile">
    <tiles:put name="widget" beanName="widget"/>
    <tiles:put name="bag" beanName="bag"/>
    <tiles:put name="widget2extraAttrs" beanName="widget2extraAttrs" />
  </tiles:insert>
</c:forEach>
<div style="clear:both;"></div>

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
      <tiles:put name="placement" value="im:aspect:${aspect}"/>
      <tiles:put name="trail" value="|bag.${bag.name}"/>
      <tiles:put name="interMineIdBag" beanName="bag"/>
      <tiles:put name="aspectId" value="${templateIdPrefix}${status.index}" />
      <tiles:put name="opened" value="${status.index == 0}" />
    </tiles:insert>
  </c:forEach>

</div>  <!-- templates body -->

<!-- /templates -->
</c:when>
<c:otherwise>
<!--  No list found with this name -->
<div class="bigmessage">
 <br />
 <html:link action="/bag?subtab=view">View all lists</html:link>
</div>
</c:otherwise>
</c:choose>
</div>  <!-- whole page body -->
<!-- /bagDetails.jsp -->
