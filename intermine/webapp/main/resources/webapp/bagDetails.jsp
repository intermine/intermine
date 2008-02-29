<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1" prefix="str" %>

<!-- bagDetails.jsp -->
<html:xhtml/>
<link rel="stylesheet" href="css/resultstables.css" type="text/css" />
<script type="text/javascript">
  <!--//<![CDATA[
      var modifyDetailsURL = '<html:rewrite action="/modifyDetails"/>';
      var detailsType = 'bag';

  function go(where) {
    switch (where){
      case "query":
        document.modifyBagDetailsForm.useBagInQuery.value = 'true';
        break;
    }    
      document.modifyBagDetailsForm.submit();
     }


      //]]>-->
</script>
<script type="text/javascript" src="js/inlinetemplate.js">
  var modifyDetailsURL = '<html:rewrite action="/modifyDetails"/>';
</script>


<div class="heading">
     <b>${bag.name}</b> (${bag.size} ${bag.type}s)
</div>

<div class="body">


<TABLE cellspacing=20>
<TR>


<TD colspan=2 align="left">


<link rel="stylesheet" href="css/toolbar.css" type="text/css" media="screen" title="Toolbar Style" charset="utf-8">
<script type="text/javascript" src="js/toolbar.js"></script>
<html:form action="/modifyBagDetailsAction" styleId="bagDetailsForm">
<html:hidden property="bagName" value="${bag.name}"/> 

<div id="tool_bar_div">
    <ul id="button_bar" onclick="toggleToolBarMenu(event);">
        <%-- <li id="tool_bar_li_convert"><img style="cursor: pointer;" src="images/icons/null.gif" width="94" height="25" alt="Convert" border="0" id="tool_bar_button_convert" class="tool_bar_button"></li> --%>
        <li id="tool_bar_li_display"><img style="cursor: pointer;" src="images/icons/null.gif" width="62" height="25" alt="Display" border="0" id="tool_bar_button_display" class="tool_bar_button"></li>
        <li id="tool_bar_li_export"><img style="cursor: pointer;" src="images/icons/null.gif" width="64" height="25" alt="Export" border="0" id="tool_bar_button_export" class="tool_bar_button"></li>
        <li id="tool_bar_li_use"><img style="cursor: pointer;" src="images/icons/null.gif" width="43" height="25" alt="Use" border="0" id="tool_bar_button_use" class="tool_bar_button"></li>
    </ul>
</div>

<%-- <div id="tool_bar_item_convert" style="visibility:hidden" class="tool_bar_item">
  <tiles:insert name="convertBag.tile">
       <tiles:put name="bag" beanName="bag" />
       <tiles:put name="idname" value="bar" />
  </tiles:insert>
    <hr>
  <a href="javascript:hideMenu('tool_bar_item_convert')" >Cancel</a>
</div> --%>
<div id="tool_bar_item_display" style="visibility:hidden" class="tool_bar_item">
    <html:link anchor="relatedTemplates" action="bagDetails?bagName=${bag.name}">related templates</html:link><br/>
    <html:link anchor="widgets" action="bagDetails?bagName=${bag.name}">related widgets</html:link>
    <hr>
  <a href="javascript:hideMenu('tool_bar_item_display')">Cancel</a>
</div>
<div id="tool_bar_item_export" style="visibility:hidden" class="tool_bar_item">
      <a href="exportAction.do?table=${bag.name}&type=tab&tableType=bag">tab-separated</a><br/>
    <a href="exportAction.do?table=${bag.name}&type=csv&tableType=bag">comma-separated</a><br/>
    <a href="exportAction.do?table=${bag.name}&type=excel&tableType=bag">excel</a>
  <hr>
  <a href="javascript:hideMenu('tool_bar_item_export')" >Cancel</a>
</div>
<div id="tool_bar_item_use" style="visibility:hidden" class="tool_bar_item">
    <a href="javascript:go('query');">in a query</a><br/>
  <input type="hidden" name="useBagInQuery" />
  <html:link action="/templates">in a template</html:link>
  <hr>
    <a href="javascript:hideMenu('tool_bar_item_use')" >Cancel</a>
</div>
</html:form>

</TD>
</TR>
<TR>

<TD valign="top">
<div>

<%-- Table displaying bag elements --%>
<tiles:insert name="resultsTable.tile">
     <tiles:put name="pagedResults" beanName="pagedResults" />
</tiles:insert>
<c:if test="${!empty bag.dateCreated}">
    <i><b>Created:</b> <im:dateDisplay date="${bag.dateCreated}" /></i>
</c:if>
<html:submit property="showInResultsTable">
    View all ${bag.size} records >>
</html:submit>
</div>

<div id="clearLine">&nbsp;</div>

<div style="clear:both">

<%-- Bag Description --%>
<c:choose>
    <c:when test="${myBag == 'true'}">
      <div id="bagDescriptionDiv" onclick="Element.toggle('bagDescriptionDiv');Element.toggle('bagDescriptionTextarea');$('textarea').focus()">
        <c:choose>
          <c:when test="${! empty bag.description}">
            <c:out value="${bag.description}" escapeXml="false" />
          </c:when>
          <c:otherwise>
            <div id="emptyDesc">Click here to enter a description for your list.</div>
          </c:otherwise>
        </c:choose>
      </div>
      <div id="bagDescriptionTextarea" style="display:none">
        <textarea id="textarea"><c:if test="${! empty bag.description}"><c:out value="${fn:replace(bag.description,'<br/>','')}" /></c:if></textarea>
        <div>
          <button onclick="Element.toggle('bagDescriptionTextarea');
              Element.toggle('bagDescriptionDiv'); return false;">Cancel</button>
          <button onclick="saveBagDescription('${bag.name}'); return false;">Save</button>
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

<TD align="left" valign="top">

<div id="convertList" class="pageDesc" align="left">
<h3>Convert</h3>
<html:form action="/modifyBagDetailsAction" styleId="bagDetailsForm">
<html:hidden property="bagName" value="${bag.name}"/> 
<tiles:insert name="convertBag.tile">
     <tiles:put name="bag" beanName="bag" />
     <tiles:put name="idname" value="cp" />
     <tiles:put name="orientation" value="h" />
</tiles:insert>
</html:form>
</div>
<script type="text/javascript">
        Nifty("div#convertList","transparent");
</script>

    <tiles:insert page="/bagDisplayers.jsp">
      <tiles:put name="bag" beanName="bag"/>
    </tiles:insert>

</TD></TR>
</TABLE>

<div id="clearLine">&nbsp;</div>


<!-- widget table -->
<c:set var="widgetIdPrefix" value="bagDetailsWidget${bag.type}"/>
<c:set var="widgetTotal" value="${fn:length(graphDisplayerArray) 
                + fn:length(tableDisplayerArray)  
                + fn:length(enrichmentWidgetDisplayerArray) }"/>

  <c:if test="${widgetTotal > 0}">
    <div class="heading">
      <a id="widgets">Widgets displaying properties of '${bag.name}'</a>&nbsp;&nbsp;<span style="font-size:0.8em;">
    (<a href="javascript:toggleAll(${widgetTotal}, '${widgetIdPrefix}', 'expand', null, true);">expand all <img src="images/disclosed.gif"/></a> / <a href="javascript:toggleAll(${widgetTotal}, '${widgetIdPrefix}', 'collapse', null, true);">collapse all <img src="images/undisclosed.gif"/></a>)</span>
    </div>
    <div class="body">
  
      <fmt:message key="bagDetails.widgetsHelp">
        <fmt:param>
              <img src="images/disclosed.gif"/> / <img src="images/undisclosed.gif"/>  
        </fmt:param>
      </fmt:message>
    
      <c:set var="widgetCount" value="0"/>
      
      <%-- graphs --%>
      <c:forEach items="${graphDisplayerArray}" var="htmlContent">      
      <imutil:disclosure id="${widgetIdPrefix}${widgetCount}" opened="true" type="consistent">
        <imutil:disclosureHead>
          <imutil:disclosureTitle>
            ${htmlContent[1]}
          </imutil:disclosureTitle>
        </imutil:disclosureHead>
        <imutil:disclosureBody>
            <br/>    
          <fmt:message key="bagDetails.widgetHelp">
              <fmt:param>
                    <fmt:message key="bagDetails.widgetHelpGraph"/>
              </fmt:param>
            </fmt:message>
              <br/><br/>  
        
              <div class="widget">
                <c:out value="${htmlContent[0]}" escapeXml="false"/>
                <p><c:out value="${htmlContent[2]}" escapeXml="false"/></p>
              </div>
              <c:set var="widgetCount" value="${widgetCount+1}" />        
        </imutil:disclosureBody>
      </imutil:disclosure>
      </c:forEach>

  <%-- tables --%>
      <c:forEach items="${tableDisplayerArray}" var="bagTableDisplayerResults">
        
        <imutil:disclosure id="${widgetIdPrefix}${widgetCount}" opened="true" type="consistent">
      <imutil:disclosureHead>
        <imutil:disclosureTitle>
          <c:out value="${bagTableDisplayerResults.title}"/>
        </imutil:disclosureTitle>
      </imutil:disclosureHead>
      <imutil:disclosureBody>
              <br/>
          <fmt:message key="bagDetails.widgetHelp">
            <fmt:param>
                  <fmt:message key="bagDetails.widgetHelpTable"/>
            </fmt:param>
          </fmt:message>
            <br/><br/>
            
              <div><strong><font size="+1"><c:out value="${bagTableDisplayerResults.title}"/></font></strong></div>
              <c:choose>
                <c:when test="${!empty bagTableDisplayerResults.flattenedResults}">
                    <div class="widget_slide">
                    <table class="results" cellspacing="0">
                      <tr>
                        <c:forEach var="column" items="${bagTableDisplayerResults.columns}" varStatus="status">
                          <th align="center" valign="top">
                            <div>
                              <c:out value="${fn:replace(column, '.', '&nbsp;> ')}" escapeXml="false"/>
                            </div>
                          </th>
                        </c:forEach>
                      </tr>
    
                      <c:forEach items="${bagTableDisplayerResults.flattenedResults}" var="row" varStatus="status">
                        <c:set var="rowClass">
                          <c:choose>
                            <c:when test="${status.count % 2 == 1}">odd</c:when>
                            <c:otherwise>even</c:otherwise>
                          </c:choose>
                        </c:set>
                        <tr class="${rowClass}">
                          <c:forEach var="cell" items="${row}" varStatus="status2">
                            <td>
                              <c:choose>
                                <c:when test="${cell.keyField}">
                                  <html:link action="/objectDetails?id=${cell.id}&amp;trail=|bag.${bag.name}|${cell.id}">
                                    <c:out value="${cell.field}" />
                                  </html:link>
                                </c:when>
                                <c:when test="${! empty cell.otherLink}">
                                  <html:link action="/widgetAction?${cell.otherLink}">
                                    <c:out value="${cell.field}" />
                                  </html:link>
                                </c:when>
                                <c:otherwise>
                                  <c:out value="${cell.field}" />
                                </c:otherwise>
                              </c:choose>
                            </td>
                          </c:forEach>
                        </tr>
                      </c:forEach>
                    </table>
                    <p><c:out value="${bagTableDisplayerResults.description}" escapeXml="false"/></p>
                  </div>
                </c:when>
                <c:otherwise><i>No results for ${bagTableDisplayerResults.title}</i></c:otherwise>
              </c:choose>
            <c:set var="widgetCount" value="${widgetCount+1}" />
      </imutil:disclosureBody>
    </imutil:disclosure>
      </c:forEach>

    
   <%-- enrichment --%>
  <c:forEach items="${enrichmentWidgetDisplayerArray}" var="enrichmentWidgetResults">
  
    <imutil:disclosure id="${widgetIdPrefix}${widgetCount}" opened="true" type="consistent">
      <imutil:disclosureHead>
        <imutil:disclosureTitle>
          <c:out value="${enrichmentWidgetResults.title}"/>
        </imutil:disclosureTitle>
      </imutil:disclosureHead>
    <imutil:disclosureBody>
       <br/>
       <fmt:message key="bagDetails.widgetHelp">
            <fmt:param>
                  <fmt:message key="bagDetails.widgetHelpTable"/>
            </fmt:param>
          </fmt:message> 
          <br/><br/>
          <str:encodeUrl var="externalLink">${enrichmentWidgetResults.externalLink}</str:encodeUrl>
          <c:set var="enrichmentWidgetParams" value="bagName=${bag.name}&ldr=${enrichmentWidgetResults.ldr}&title=${enrichmentWidgetResults.title}&descr=${enrichmentWidgetResults.descr}&max=${enrichmentWidgetResults.max}&link=${enrichmentWidgetResults.link}&filters=${enrichmentWidgetResults.filters}&filterLabel=${enrichmentWidgetResults.filterLabel}&label=${enrichmentWidgetResults.label}&externalLink=${externalLink}"/>
          <iframe src="enrichmentWidget.do?${enrichmentWidgetParams}" id="window" frameborder="0" width="475" height="500" scrollbars="auto"></iframe>
        <br/><a href="enrichmentWidget.do?${enrichmentWidgetParams}" target="_new" class="extlink">open widget in new window</a>
       <c:set var="widgetCount" value="${widgetCount+1}" />
    </imutil:disclosureBody>
    </imutil:disclosure>
  </c:forEach>
  </div>
</c:if>

<!-- /widgets -->

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
