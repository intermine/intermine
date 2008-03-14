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
  <h3>${widget.title}</h3>
  <p>${widget.description}<br/>
  <c:if test="${fn:length(widget2extraAttrs[widget.id])>0}">
    <select name="widgetselect${widget.id}" id="widgetselect${widget.id}" onchange="getProcessGraphWidget('${widget.id}','${bag.name}',this.value);">
    <c:forEach items="${widget2extraAttrs[widget.id]}" var="extraParams">
      <option value="${extraParams}">${extraParams}</option>
    </c:forEach>
    </select>
  </c:if>
  </p>
  <div id="widgetdata${widget.id}" class="widgetdata"></div>
  <div id="widgetdatawait${widget.id}" class="widgetdatawait"><img src="images/wait30.gif" title="Searching..."/></div>
  <script language="javascript">
  <c:choose>
    <c:when test="${fn:contains(widget.class,'GraphWidget')}" >
        <!--//<![CDATA[
           getProcessGraphWidget('${widget.id}','${bag.name}','${widget.selectedExtraAttribute}');
        //]]>-->
    </c:when>
    <c:when test="${fn:contains(widget.class,'TableWidget')}" >
    <!--//<![CDATA[
        function getProcessWidget(widgetId, bagName) {
          AjaxServices.getProcessTableWidget(widgetId,bagName,handleGetWidget)
        }
        
        getWidget('${widget.id}','${bag.name}'};
        handleGetWidget('${widget.id}','${bag.name}'};
    //]]>-->
    </c:when>
    <c:when test="${fn:contains(widget.class,'EnrichmentWidget')}" >
    <!--//<![CDATA[
        function getProcessWidget(widgetId, bagName) {
          AjaxServices.getProcessEnrichmentWidget(widgetId,bagName,handleGetWidget)
        }
        
        getWidget('${widget.id}','${bag.name}'};
        handleGetWidget('${widget.id}','${bag.name}'};
    //]]>-->
    </c:when>
  </c:choose>
  </script>
</div>

<!-- /widget.jsp -->