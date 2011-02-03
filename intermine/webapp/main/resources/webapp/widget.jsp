<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
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
<html:hidden property="widgetTitle" value="${widget.title}" />
<html:hidden property="action" value="" styleId="action${widget.id}"/>
<html:hidden property="exporttype" value="" styleId="export${widget.id}"/>

<c:set var="extraAttrMap" value="${widget2extraAttrs[widget.id]}" />

<div id="widgetcontainer${widget.id}" class="widgetcontainer">

  <span id="closewidget${widget.id}" class="widgetcloser"><a href="javascript:toggleWidget('widgetcontainer${widget.id}','togglelink${widget.id}');">close</a></span>
  <h3>${widget.title}</h3>
  <p>${widget.description}
  <c:if test="${type == 'EnrichmentWidgetConfig'}">
    For more information about the math used in these calculations, see <a href="http://www.intermine.org/wiki/EnrichmentWidgets">here</a>.
  </c:if>
  <BR/>
  <c:set var="isMSIE" value='<%= new Boolean(request.getHeader("user-agent").indexOf("MSIE") != -1) %>'/>
  <c:if test="${type ne 'HTMLWidgetConfig' && !isMSIE}" >
    <span style="margin-top:5px">Number of ${bag.type}s in this list not analysed in this widget:  <span id="widgetnotanalysed${widget.id}"><%--${widget.notAnalysed}--%></span></span>
  </c:if>
 </p>

 <c:if test="${type == 'EnrichmentWidgetConfig' || (fn:length(extraAttrMap)>0)}" >
  <fieldset>
  <legend>Options</legend>
  <ol>
   <c:if test="${type == 'EnrichmentWidgetConfig'}" >

   <html:hidden property="externalLink${widget.id}" styleId="externalLink${widget.id}" value="${widget.externalLink}"/>
   <html:hidden property="externalLinkLabel${widget.id}" styleId="externalLinkLabel${widget.id}" value="${widget.externalLinkLabel}"/>
    <li>
    <label>Multiple Hypothesis Test Correction</label>
    <html:select property="errorCorrection" styleId="errorCorrection${widget.id}" onchange="getProcessEnrichmentWidgetConfig('${widget.id}','${bag.name}');">
      <html:option value="Holm-Bonferroni">Holm-Bonferroni</html:option>
      <html:option value="Benjamini Hochberg">Benjamini and Hochberg</html:option>
      <html:option value="Bonferroni">Bonferroni</html:option>
      <html:option value="None">None</html:option>
    </html:select>
    </li>
    <li style="float:right">
    <label>Maximum value to display</label>
    <html:select property="max" styleId="max${widget.id}" onchange="getProcessEnrichmentWidgetConfig('${widget.id}','${bag.name}')">
      <html:option value="0.05">0.05</html:option>
      <html:option value="0.10">0.10</html:option>
      <html:option value="1.00">1.00</html:option>
    </html:select>
    </li>
   </c:if>
    <c:forEach items="${extraAttrMap}" var="entry">
    <c:if test="${! empty entry.key && entry.key != 'Editable'}">
      <li>
        <label>${entry.key}:</label>
        <html:select property="selectedExtraAttribute" styleId="widgetselect${widget.id}" onchange="getProcess${type}('${widget.id}','${bag.name}');">
        <c:forEach items="${entry.value}" var="extraParams">
          <%--<c:choose>
            <c:when test="${widget.selectedExtraAttribute == extraParams}">
              <option value="${extraParams}" selected>${extraParams}</option>
            </c:when>
            <c:otherwise>--%>
              <html:option value="${extraParams}">${extraParams}</html:option>
            <%--</c:otherwise>
          </c:choose>--%>
        </c:forEach>
        </html:select>
      </li>
    </c:if>
  </c:forEach>
  </ol>
  </fieldset>
 </c:if>
 <c:if test="${(type == 'EnrichmentWidgetConfig' || type == 'TableWidgetConfig') && !empty widget.link}">
  <div id="widget_tool_bar_div_${widget.id}" class="widget_tool_bar_div" >
    <ul id="widget_button_bar_${widget.id}" class="widget_button_bar" >
        <!-- View in results table button -->
        <li id="tool_bar_li_display_widget_${widget.id}" class="tb_button">
          <span id="tool_bar_button_display_${widget.id}" class="widget_tool_bar_button"
          onclick="jQuery('#tool_bar_item_display_widget_${widget.id}').toggle();return false;"
          >View in results table</span>
        </li>
        <li id="tool_bar_li_export_widget_${widget.id}" class="tb_button">
          <span id="tool_bar_button_export_${widget.id}" class="widget_tool_bar_button"
          onclick="jQuery('#tool_bar_item_export_widget_${widget.id}').toggle();return false;"
          >Download</span>
        </li>
    </ul>
  </div>
  <!-- View in results table table -->
  <div id="tool_bar_item_display_widget_${widget.id}" style="display:none;width:200px;text-align:left" class="tool_bar_item">
    <a href="javascript:submitWidgetForm('${widget.id}','display',null)">Display checked items in results table</a><br/>
    <a href="javascript:submitWidgetForm('${widget.id}','displayAll',null)">Display all items in results table</a>
    <hr/>
    <a href="#" onclick="jQuery('#tool_bar_item_display_widget_${widget.id}').toggle();return false;">Cancel</a>
  </div>

  <div id="tool_bar_item_export_widget_${widget.id}" style="display:none;width:230px;text-align:left" class="tool_bar_item">
    <a href="javascript:submitWidgetForm('${widget.id}','export','csv')">Export selected as comma separated values</a><br/>
    <a href="javascript:submitWidgetForm('${widget.id}','export','tab')">Export selected as tab separated values</a>
    <hr/>
  <a href="#" onclick="jQuery('#tool_bar_item_export_widget_${widget.id}').toggle();return false;">Cancel</a>
  </div>
 </c:if>

<%-- output different widget containers if it's a graph widget because flyatlas widget is too tall --%>

<c:choose>
  <c:when test="${type == 'GraphWidgetConfig'}" >
    <div id="widgetdata${widget.id}" class="widgetdata">
  </c:when>
  <c:otherwise>
    <div id="widgetdata${widget.id}" class="widgetdataoverflow" style="${widget.style}">
  </c:otherwise>
</c:choose>

    <c:if test="${type ne 'GraphWidgetConfig'}" >
      <table id="tablewidget${widget.id}" border="0" >
        <thead id="tablewidget${widget.id}head"></thead>
        <tbody id="tablewidget${widget.id}body"></tbody>
      </table>
    </c:if>
  </div>
  <div id="widgetdatawait${widget.id}" class="widgetdatawait"><img src="images/wait30.gif" title="Searching..."/></div>
  <div id="widgetdatanoresults${widget.id}" class="widgetdatawait" style="display:none;"><i>no results found</i></div>
  <c:if test="${type == 'HTMLWidgetConfig'}" >
    <div id="widgetdatacontent${widget.id}" class="widgetdatawait" style="display:none;">${widget.content}</div>
  </c:if>
  <script language="javascript">
  <c:choose>
    <c:when test="${type == 'GraphWidgetConfig'}" >

      getProcessGraphWidgetConfig('${widget.id}','${bag.name}');

    </c:when>
    <c:when test="${type == 'TableWidgetConfig'}" >

      getProcessTableWidgetConfig('${widget.id}','${bag.name}');

    </c:when>
    <c:when test="${type == 'EnrichmentWidgetConfig'}" >

      getProcessEnrichmentWidgetConfig('${widget.id}','${bag.name}');

    </c:when>
    <c:when test="${type == 'HTMLWidgetConfig'}" >

      getProcessHTMLWidgetConfig('${widget.id}','${bag.name}');

    </c:when>
  </c:choose>

  </script>
</div>
</html:form>
<!-- /widget.jsp -->
