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

<script type="text/javascript">
	       //if key is Enter
	       
		   function isEnter(e, widgetid, bagname, ajax) { 
	        var curKey;
	    	if (e.which) {   			// FF
	        	curKey = e.which;
	      	} else if (e.keyCode) {     // IE
	      		curKey = e.keyCode;
	      	}
	    	//enter
	    	if (curKey == 13) {
	    		Event.stop(e); 
	            callAJAX(widgetid, bagname, ajax);
	        }
	       }
	       //only doubles are allowed
	       
	       function onlyDouble(d)
	       {
			  var val = d.value.replace(/[^\.^,\d]/g, '');
			  d.value = val;
		   }
	       //called the ajax service
	       
		   function callAJAX(widgetid, bagname, ajax){
		     $('pValue'+widgetid).value;
		     if(ajax == 'grid') {
		     	getProcessGridWidgetConfig(widgetid, bagname);
		     } else if(ajax == 'graph') {
		     	getProcessGraphWidgetConfig(widgetid, bagname);
		     }
		   }
</script>
 <c:set var="extraAttrMap" value="${widget2extraAttrs[widget.id]}" />
<c:if test="${type != 'GridWidgetConfig'}" >
<div id="widgetcontainer${widget.id}" class="widgetcontainer">
</c:if>
<c:if test="${type == 'GridWidgetConfig'}" >
	<c:forEach items="${extraAttrMap}" var="entry">
	<c:if test="${entry.key == 'Width'}">
	<c:forEach items="${entry.value}" var="tWidth">
	<div id="widgetcontainer${widget.id}" class="widgetcontainer" style="width:${tWidth}">
	</c:forEach>
	</c:if>
	</c:forEach>
</c:if>
  <span id="closewidget${widget.id}" class="widgetcloser"><a href="javascript:toggleWidget('widgetcontainer${widget.id}','togglelink${widget.id}');">close x</a></span>
  <h3>${widget.title}</h3>
  <p>${widget.description}<br/>
  <span style="margin-top:5px">Number of ${bag.type}s in this list not analysed in this widget:
<%-- hide until table and graph widgets can handle this link
	<c:choose>
	<c:when test="${type == 'EnrichmentWidgetConfig'}">
    	<a href="javascript:displayNotAnalysed(${widget.id})"><span id="widgetnotanalysed${widget.id}">${widget.notAnalysed}</span></a>
	</c:when>
	<c:otherwise>
 --%>
    	<span id="widgetnotanalysed${widget.id}"><%--${widget.notAnalysed}--%></span>
<%--
    </c:otherwise>
	</c:choose>
	--%>
	</span>
 </p>

 <c:if test="${type == 'EnrichmentWidgetConfig' || (fn:length(extraAttrMap)>0 && type != 'GridWidgetConfig')}" >
  <fieldset>
  <legend>Options</legend>
  <ol>
   <c:if test="${type == 'EnrichmentWidgetConfig'}" >

   <html:hidden property="externalLink${widget.id}" styleId="externalLink${widget.id}" value="${widget.externalLink}"/>
   <html:hidden property="externalLinkLabel${widget.id}" styleId="externalLinkLabel${widget.id}" value="${widget.externalLinkLabel}"/>
    <li>
    <label>Multiple Hypothesis Test Correction</label>
    <html:select property="errorCorrection" styleId="errorCorrection${widget.id}" onchange="getProcessEnrichmentWidgetConfig('${widget.id}','${bag.name}');">
      <html:option value="Benjamini and Hochberg">Benjamini and Hochberg</html:option>
      <html:option value="Bonferroni">Bonferroni</html:option>
      <html:option value="None">None</html:option>
    </html:select>
    </li>
    <li style="float:right">
    <label>Maximum value to display</label>
    <html:select property="max" styleId="max${widget.id}" onchange="getProcessEnrichmentWidgetConfig('${widget.id}','${bag.name}')">
      <html:option value="0.01">0.01</html:option>
      <html:option value="0.05">0.05</html:option>
      <html:option value="0.10">0.10</html:option>
      <html:option value="0.50">0.50</html:option>
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
	   <c:if test="${! empty entry.key && entry.key == 'Editable'}">
	    <li>
	    <label>Maximum p-value to display</label>
	    <form action="false">
	    <input id="pValue${widget.id}" name="pValue(${widget.id})" size = "5" value="0.01" onKeyUp="onlyDouble(this);" onKeyDown="isEnter(event, '${widget.id}', '${bag.name}', 'graph');" />
	    <input type="button" name="GO" value="GO" onclick="callAJAX('${widget.id}', '${bag.name}', 'graph')">
	    </form>
	    </li>
	  </c:if>
	</c:forEach>
  </ol>
  </fieldset>
 </c:if>
 <c:if test="${type == 'EnrichmentWidgetConfig' || type == 'TableWidgetConfig'}">
  <div id="widget_tool_bar_div_${widget.id}" class="widget_tool_bar_div" >
    <ul id="widget_button_bar_${widget.id}" onclick="toggleToolBarMenu(event,'widget');" class="widget_button_bar" >
        <li id="tool_bar_li_display_${widget.id}"><span id="tool_bar_button_display_${widget.id}" class="widget_tool_bar_button">Display</span></li>
        <li id="tool_bar_li_export_${widget.id}"><span id="tool_bar_button_export_${widget.id}" class="widget_tool_bar_button">Export</span></li>
    </ul>
  </div>
  <div id="tool_bar_item_display_${widget.id}" style="visibility:hidden;width:200px" class="tool_bar_item">
    <a href="javascript:submitWidgetForm('${widget.id}','display',null)">Display checked items in results table</a>
    <hr/>
    <a href="javascript:hideMenu('tool_bar_item_display_${widget.id}','widget')" >Cancel</a>
  </div>

  <div id="tool_bar_item_export_${widget.id}" style="visibility:hidden;width:230px" class="tool_bar_item">
    <a href="javascript:submitWidgetForm('${widget.id}','export','csv')">Export selected as comma separated values</a><br/>
    <a href="javascript:submitWidgetForm('${widget.id}','export','tab')">Export selected as tab separated values</a>
    <hr/>
  <a href="javascript:hideMenu('tool_bar_item_export_${widget.id}','widget')" >Cancel</a>
  </div>
 </c:if>
  <c:if test="${type == 'GridWidgetConfig'}" >
  <fieldset>
  <legend>Options</legend>
	   <div>
	   <c:forEach items="${extraAttrMap}" var="entry">
	   <c:if test="${! empty entry.key && entry.key == 'Editable'}">
	   <li>
	   <label>Maximum p-value to display</label>
	   <form action="false">
	   <input id="pValue${widget.id}" name="pValue(${widget.id})" size = "5" value="0.01" onKeyUp="onlyDouble(this);" onKeyDown="isEnter(event, '${widget.id}', '${bag.name}', 'grid');" />
	   <input type="button" name="GO" value="GO" onclick="callAJAX('${widget.id}', '${bag.name}', 'grid')">
	   </form>
	   </li>
	   </c:if>
       	</c:forEach>
	   <c:forEach items="${extraAttrMap}" var="entry">
	   <c:if test="${entry.key != 'Editable' && entry.key != 'Width'}">
	    <li>
	    <label>Show numbers as</label>
	      <html:select property="numberOpt" styleId="numberOpt${widget.id}" onchange="getProcessGridWidgetConfig('${widget.id}','${bag.name}');">
      		<html:option value="number">number</html:option>
      		<html:option value="percentage">percentage</html:option>
       	</html:select>
	      <label>Using highlighting for</label>
	      <html:select property="highlight" styleId="highlight${widget.id}" onchange="getProcessGridWidgetConfig('${widget.id}','${bag.name}');">
	      <c:forEach items="${entry.value}" var="extraParams">
	            <html:option value="${extraParams}">${extraParams}</html:option>
	      </c:forEach>
       	</html:select>
       	</li>
       	</c:if>
       	</c:forEach>
	   </div>
  </fieldset> 
  <fieldset style="text-align:center">
  <legend>Color legend</legend>
   <div id="gridimage${widget.id}" class="gridwidget"><img src="model/images/intersec.jpg" title="colorbar"/></div>
   </fieldset>
 </c:if>
 <c:if test="${type == 'GridWidgetConfig'}" >
 	<c:forEach items="${extraAttrMap}" var="entry">
 <c:if test="${! empty entry.key && entry.key == 'Width'}">
  <c:forEach items="${entry.value}" var="tWidth">
  <div id="widgetdata${widget.id}" class="widgetdata" style="width:${tWidth}">
  </c:forEach>
  </c:if>
  </c:forEach>
  </c:if>
  <c:if test="${type != 'GridWidgetConfig'}" >
  <div id="widgetdata${widget.id}" class="widgetdata">
  </c:if>
    <c:if test="${type == 'TableWidgetConfig' || type == 'EnrichmentWidgetConfig' || type == 'GridWidgetConfig' }" >
      <c:if test="${type == 'GridWidgetConfig'}" >
      	<table id="tablewidget${widget.id}" border="1">
      </c:if>
      <c:if test="${type == 'TableWidgetConfig' || type == 'EnrichmentWidgetConfig'}" >
      	<table id="tablewidget${widget.id}" border="0" >
      </c:if>
        <thead id="tablewidget${widget.id}head"></thead>
        <tbody id="tablewidget${widget.id}body"></tbody>
      </table>
    </c:if>
  </div>
  <div id="widgetdatawait${widget.id}" class="widgetdatawait"><img src="images/wait30.gif" title="Searching..."/></div>
  <div id="widgetdatanoresults${widget.id}" class="widgetdatawait" style="display:none;"><i>no results found</i></div>
  <script language="javascript">
  <c:choose>
    <c:when test="${type == 'GraphWidgetConfig'}" >
        <!--//<![CDATA[
           getProcessGraphWidgetConfig('${widget.id}','${bag.name}');
        //]]>-->
    </c:when>
    <c:when test="${type == 'TableWidgetConfig'}" >
    <!--//<![CDATA[
           getProcessTableWidgetConfig('${widget.id}','${bag.name}');
    //]]>-->
    </c:when>
    <c:when test="${type == 'EnrichmentWidgetConfig'}" >
    <!--//<![CDATA[
           getProcessEnrichmentWidgetConfig('${widget.id}','${bag.name}');
    //]]>-->
    </c:when>
    <c:when test="${type == 'GridWidgetConfig'}" >
    <!--//<![CDATA[
           getProcessGridWidgetConfig('${widget.id}','${bag.name}');
    //]]>-->
    </c:when>
  </c:choose>
  </script>
</div>
</html:form>
<!-- /widget.jsp -->