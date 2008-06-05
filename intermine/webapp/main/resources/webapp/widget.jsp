<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1" prefix="str" %>

<!-- widget.jsp -->

<tiles:importAttribute name="widget" ignore="false" />
<tiles:importAttribute name="bag" ignore="false" />
<tiles:importAttribute name="widget2extraAttrs" ignore="false" />

<c:set var="split" value="${fn:split(widget.class,'.')}"/>
<c:set var="type" value="${split[fn:length(split)-1]}"/>

<html:xhtml/>
<html:form action="/widgetAction" styleId="widgetaction${widget.id}">
<html:hidden property="link" value="${widget.link}"/>
<html:hidden property="bagType" value="${bag.type}"/>
<html:hidden property="bagName" value="${bag.name}" />
<html:hidden property="widgetid" value="${widget.id}" />
<html:hidden property="action" value="" styleId="action${widget.id}"/>
<html:hidden property="exporttype" value="" styleId="export${widget.id}"/>

<div id="widgetcontainer${widget.id}" class="widgetcontainer">
  <span id="closewidget${widget.id}" class="widgetcloser"><a href="javascript:toggleWidget('widgetcontainer${widget.id}','togglelink${widget.id}');">close x</a></span>
  <h3>${widget.title}</h3>
  <p>${widget.description}<br/>
  <span style="margin-top:5px">Number of ${bag.type}s in this list not analysed in this widget:
<%-- disable until links configured for table and graph widgets
	<c:choose>
	<c:when test="${type == 'EnrichmentWidget'}">
    	<a href="javascript:displayNotAnalysed(${widget.id})"><span id="widgetnotanalysed${widget.id}">${widget.notAnalysed}</span></a>
	</c:when>
	<c:otherwise>
	  --%>
    	<span id="widgetnotanalysed${widget.id}">${widget.notAnalysed}</span>
    	<%--
    </c:otherwise>
	</c:choose></span>
	 --%>
 </p>
 <c:set var="extraAttrMap" value="${widget2extraAttrs[widget.id]}" />
 <c:if test="${type == 'EnrichmentWidget' || fn:length(extraAttrMap)>0}" >
  <fieldset>
  <legend>Options</legend>
  <ol>
   <c:if test="${type == 'EnrichmentWidget'}" >

   <html:hidden property="externalLink${widget.id}" styleId="externalLink${widget.id}" value="${widget.externalLink}"/>
   <html:hidden property="externalLinkLabel${widget.id}" styleId="externalLinkLabel${widget.id}" value="${widget.externalLinkLabel}"/>
    <li>
    <label>Multiple Hypothesis Test Correction</label>
    <select name="errorCorrection${widget.id}" id="errorCorrection${widget.id}" onchange="getProcessEnrichmentWidget('${widget.id}','${bag.name}');">
      <option value="Benjamini and Hochberg">Benjamini and Hochberg</option>
      <option value="Bonferroni">Bonferroni</option>
      <option value="None">None</option>
    </select>
    </li>
    <li style="float:right">
    <label>Maximum value to display</label>
    <select name="max${widget.id}" id="max${widget.id}" onchange="getProcessEnrichmentWidget('${widget.id}','${bag.name}')">
      <option value="0.01">0.01</option>
      <option value="0.05">0.05</option>
      <option value="0.10">0.10</option>
      <option value="0.50">0.50</option>
      <option value="1.00">1.00</option>
    </select>
    </li>
   </c:if>
    <c:forEach items="${extraAttrMap}" var="entry">
	  <c:if test="${! empty entry.key}">
	    <li>
	      <label>${entry.key}:</label>
	      <select name="widgetselect${widget.id}" id="widgetselect${widget.id}" onchange="getProcess${type}('${widget.id}','${bag.name}');">
	      <c:forEach items="${entry.value}" var="extraParams">
	        <c:choose>
	          <c:when test="${widget.selectedExtraAttribute == extraParams}">
	            <option value="${extraParams}" selected>${extraParams}</option>
	          </c:when>
	          <c:otherwise>
	            <option value="${extraParams}">${extraParams}</option>
	          </c:otherwise>
	        </c:choose>
	      </c:forEach>
	      </select>
	    </li>
	  </c:if>
	</c:forEach>
  </ol>
  </fieldset>
 </c:if>
 <c:if test="${type == 'EnrichmentWidget' || type == 'TableWidget'}">
  <div id="widget_tool_bar_div_${widget.id}" class="widget_tool_bar_div" >
    <ul id="widget_button_bar_${widget.id}" onclick="toggleToolBarMenu(event,'widget');" class="widget_button_bar" >
        <li id="tool_bar_li_display_${widget.id}"><span id="tool_bar_button_display_${widget.id}" class="widget_tool_bar_button">Display</span></li>
        <li id="tool_bar_li_export_${widget.id}"><span id="tool_bar_button_export_${widget.id}" class="widget_tool_bar_button">Export</span></li>
    </ul>
  </div>
  <div id="tool_bar_item_display_${widget.id}" style="visibility:hidden;width:200px" class="tool_bar_item">
    <a href="javascript:submitWidgetForm(${widget.id},'display',null)">Display checked items in results table</a>
    <hr/>
    <a href="javascript:hideMenu('tool_bar_item_display_${widget.id}','widget')" >Cancel</a>
  </div>

  <div id="tool_bar_item_export_${widget.id}" style="visibility:hidden;width:230px" class="tool_bar_item">
    <a href="javascript:submitWidgetForm(${widget.id},'export','csv')">Export selected as comma separated values</a><br/>
    <a href="javascript:submitWidgetForm(${widget.id},'export','tab')">Export selected as tab separated values</a>
    <hr/>
  <a href="javascript:hideMenu('tool_bar_item_export_${widget.id}','widget')" >Cancel</a>
  </div>
 </c:if>
  <div id="widgetdata${widget.id}" class="widgetdata">
    <c:if test="${type == 'TableWidget' || type == 'EnrichmentWidget'}" >
      <table id="tablewidget${widget.id}" border="0" >
        <thead id="tablewidget${widget.id}head"></thead>
        <tbody id="tablewidget${widget.id}body"></tbody>
      </table>
    </c:if>
  </div>
  <div id="widgetdatawait${widget.id}" class="widgetdatawait"><img src="images/wait30.gif" title="Searching..."/></div>
  <div id="widgetdatanoresults${widget.id}" class="widgetdatawait" style="display:none;"><i>no results found</i></div>
  <script language="javascript">
  <c:choose>
    <c:when test="${type == 'GraphWidget'}" >
        <!--//<![CDATA[
           getProcessGraphWidget('${widget.id}','${bag.name}');
        //]]>-->
    </c:when>
    <c:when test="${type == 'TableWidget'}" >
    <!--//<![CDATA[
           getProcessTableWidget('${widget.id}','${bag.name}');
    //]]>-->
    </c:when>
    <c:when test="${type == 'EnrichmentWidget'}" >
    <!--//<![CDATA[
           getProcessEnrichmentWidget('${widget.id}','${bag.name}');
    //]]>-->
    </c:when>
  </c:choose>
  </script>
</div>
</html:form>
<!-- /widget.jsp -->