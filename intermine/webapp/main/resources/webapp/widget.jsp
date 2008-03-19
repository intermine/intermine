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

<html:xhtml/>
<div id="widgetcontainer${widget.id}" class="widgetcontainer">
  <span id="closewidget${widget.id}" class="widgetcloser"><a href="javascript:toggleWidget('widgetcontainer${widget.id}','togglelink${widget.id}');">close x</a></span>
  <h3>${widget.title}</h3>
  <p>${widget.description}<br/>
  <c:if test="${fn:length(widget2extraAttrs[widget.id])>0}">
    <select name="widgetselect${widget.id}" id="widgetselect${widget.id}" onchange="getProcessGraphWidget('${widget.id}','${bag.name}');">
    <c:forEach items="${widget2extraAttrs[widget.id]}" var="extraParams">
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
  </c:if>
  </p>
 <c:if test="${fn:contains(widget.class,'EnrichmentWidget')}" >
  <fieldset>
  <legend>Options</legend>
  <ol>
    <li>
    <label>Multiple Hypothesis Test Correction</label>
    <select name="errorCorrection${widget.id}" id="errorCorrection${widget.id}" onchange="getProcessEnrichmentWidget('${widget.id}','${bag.name}');">
      <option value="Benjamini and Hochberg">Benjamini and Hochberg</option>
      <option value="Bonferroni">Bonferroni</option>
      <option value="None">None</option>
    </select>
    </li>
    <li>
    <label>Maximum value to display</label>
    <select name="max${widget.id}" id="max${widget.id}" onchange="getProcessEnrichmentWidget('${widget.id}','${bag.name}')">
      <option value="0.01">0.01</option>
      <option value="0.05">0.05</option>
      <option value="0.10">0.10</option>
      <option value="0.50">0.50</option>
      <option value="1.00">1.00</option>
    </select>
    </li>
  </ol>
  </fieldset>
  <div id="widget_tool_bar_div_${widget.id}" class="widget_tool_bar_div" >
    <ul id="widget_button_bar_${widget.id}" onclick="toggleToolBarMenu(event);" class="widget_button_bar" >
        <li id="tool_bar_li_display_${widget.id}"><span id="tool_bar_button_display_${widget.id}" class="widget_tool_bar_button">Display</span></li>
        <li id="tool_bar_li_export_${widget.id}"><span id="tool_bar_button_export_${widget.id}" class="widget_tool_bar_button">Export</span></li>
    </ul>
  </div>
  <div id="tool_bar_item_display_${widget.id}" style="visibility:hidden" class="tool_bar_item">
    <a href="javascript:document.widgetForm.submit();">Display checked items in results table</a>
    <hr>
    <a href="javascript:hideMenu('tool_bar_item_display_${widget.id}')" >Cancel</a>
  </div>
  
  <div id="tool_bar_item_export_${widget.id}" style="visibility:hidden" class="tool_bar_item">
    <!-- EXPORT CODE GOES HERE -->
    <hr>
  <a href="javascript:hideMenu('tool_bar_item_export_${widget.id}')" >Cancel</a>
</div>
 </c:if>
  
  <div id="widgetdata${widget.id}" class="widgetdata">
    <c:if test="${fn:contains(widget.class,'TableWidget') || fn:contains(widget.class,'EnrichmentWidget')}" >
      <table id="tablewidget${widget.id}" border="0" >
        <thead id="tablewidget${widget.id}head"></thead>
        <tbody id="tablewidget${widget.id}body"></tbody>
      </table>
    </c:if>
  </div>
  <div id="widgetdatawait${widget.id}" class="widgetdatawait"><img src="images/wait30.gif" title="Searching..."/></div>
  <div id="widgetdatanoresults${widget.id}" class="widgetdatawait"><i>No Results found</i></div>
  <script language="javascript">
  <c:choose>
    <c:when test="${fn:contains(widget.class,'GraphWidget')}" >
        <!--//<![CDATA[
           getProcessGraphWidget('${widget.id}','${bag.name}');
        //]]>-->
    </c:when>
    <c:when test="${fn:contains(widget.class,'TableWidget')}" >
    <!--//<![CDATA[
           getProcessTableWidget('${widget.id}','${bag.name}');
    //]]>-->
    </c:when>
    <c:when test="${fn:contains(widget.class,'EnrichmentWidget')}" >
    <!--//<![CDATA[
           getProcessEnrichmentWidget('${widget.id}','${bag.name}');
    //]]>-->
    </c:when>
  </c:choose>
  </script>
</div>

<!-- /widget.jsp -->