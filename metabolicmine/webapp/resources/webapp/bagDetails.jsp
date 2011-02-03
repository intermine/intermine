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









<div class="bochs wide">
  <h1>
    <span><fmt:message key="bagDetails.title"/></span> ${bag.name}
    <span>of</span> ${bag.size} ${bag.type}<c:if test="${bag.size != 1}">s</c:if>
    <small>created  <fmt:formatDate dateStyle="full" timeStyle="full" value="${bag.dateCreated}" /></small>
    <div class="description">
      <c:choose>
        <c:when test="${myBag == 'true'}">
            <div id="bagDescriptionDiv" onclick="jQuery('#bagDescriptionDiv').toggle();jQuery('#bagDescriptionTextarea').toggle();jQuery('#textarea').focus()">
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
                <div class="buttons">
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
    </div>
  </h1>
  <!--
  <table>
    <tr><td>Name:</td><td><strong>${bag.name}</strong></td></tr>
    <tr><td>Created:</td><td><im:dateDisplay date="${bag.dateCreated}"/></td></tr>
    <tr><td>Size:</td><td>${bag.size} record<c:if test="${bag.size != 1}">s</c:if></td></tr>
  </table>
  -->
</div>

<div style="clear:both;"></div>

<!-- convert -->
<div id="convertList" class="bochs">
  <div class="inner">
  <h4 class="convert">Convert to a different type</h4>
  <html:form action="/modifyBagDetailsAction" styleId="bagDetailsForm">
  <html:hidden property="bagName" value="${bag.name}"/>
    <p>
      <tiles:insert name="convertBag.tile">
           <tiles:put name="bag" beanName="bag" />
           <tiles:put name="idname" value="cp" />
           <tiles:put name="orientation" value="h" />
      </tiles:insert>
    </p>
  </html:form>
  </div>
</div>

<!-- export list -->
<div class="bochs">
  <div class="inner">
  <h4 class="export">Download</h4>
  <c:set var="tableName" value="bag.${bag.name}" scope="request"/>
  <c:set var="pagedTable" value="${pagedResults}" scope="request"/>
  <tiles:get name="export.tile"/>
  </div>
</div>

<!-- orthologues in other mines -->
<div id="otherMines" class="bochs last">
  <div class="inner">
  <html:form action="/modifyBagDetailsAction" styleId="bagDetailsForm">
  <html:hidden property="bagName" value="${bag.name}"/>
    <tiles:insert page="/bagDisplayers.jsp">
        <tiles:put name="bag" beanName="bag"/>
           <tiles:put name="showOnLeft" value="false"/>
    </tiles:insert>
  </html:form>
  </div>
</div>
<script type="text/javascript">
// not wanting to touch bagDisplayers.jsp tile, add External links header if said tile comes out blank
if (jQuery('#otherMines div.externalLinks').length == 0) {
  jQuery('#otherMines form').remove();
  jQuery('#otherMines div.inner').append('<div class="externalLinks"><h3>External links</h3><div id="externalLinksClear"></div></div>');
}
</script>

<!-- tags -->
<c:if test="${PROFILE.loggedIn}">
  <div class="bochs">
  <div class="inner">
      <c:set var="taggable" value="${bag}"/>
        <tiles:insert name="inlineTagEditor.tile">
          <tiles:put name="taggable" beanName="taggable"/>
            <tiles:put name="vertical" value="true"/>
            <tiles:put name="show" value="true"/>
    </tiles:insert>
    </div>
  </div>
</c:if>

<div style="clear:both;"></div>

<tiles:insert page="/bagDisplayers.jsp">
  <tiles:put name="bag" beanName="bag"/>
  <tiles:put name="showOnLeft" value="true"/>
</tiles:insert>

<div style="clear:both;"></div>

<div id="alien">
  <div class="bochs noborder">
    <div class="inner">
    <script type="text/javascript">
      // will show/hide the results table and toolbox & change the link appropriately (text, ico)
      function toggleResults() {
        // expanding or contracting?
        if (jQuery('#results').is(":visible")) {
          jQuery("a#toggleLink").text("Show ${bag.type}<c:if test="${bag.size != 1}">s</c:if> in list");
        } else {
          jQuery("a#toggleLink").text("Hide ${bag.type}<c:if test="${bag.size != 1}">s</c:if> in list");
        }
        // toggle class
        jQuery("a#toggleLink").toggleClass('active');
        // toggle results
        jQuery('#results').toggle('slow');
        jQuery('#toolbox').toggle('slow');
      }
      // let us not forget that results will be shown on successful search and when paginating that requires synchronous call
      <c:if test="${not empty param.gotoHighlighted || not empty param.page || not empty param.table}">
        jQuery(document).ready(function() { toggleResults(); });
      </c:if>
    </script>
    <h3>
      <a id="toggleLink" onclick="toggleResults();return false;" href="#">
        Show ${bag.type}<c:if test="${bag.size != 1}">s</c:if> in list
      </a>
    </h3>
    </div>
  </div>

  <!-- list search -->
  <div class="yellow bochs">
    <div class="inner">
    <h4 class="search">Find in list</h4>
    <html:form styleId="findInListForm" action="/findInList">
      <input type="text" name="textToFind" id="textToFind"/>
      <input type="hidden" name="bagName" value="${bag.name}"/>
      <html:submit>Go</html:submit>
    </html:form>
    </div>
  </div>

  <!-- modify list -->
  <div id="toolbox" class="bochs last" style="display:none;">
    <div class="inner">
    <html:form action="/modifyBagDetailsAction" styleId="bagDetailsForm">
      <html:hidden property="bagName" value="${bag.name}"/>
      <div class="tool">
        <h5>Add records to another list</h5>
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
              <em>Login to add records to another list.</em>
            </c:otherwise>
        </c:choose>
      </div>
      <div class="tool">
        <h5>Remove records from results</h5>
        <input type="submit" name="removeFromBag" id="removeFromBag" value="Remove selected from list" disabled="true" />
      </div>
    </html:form>
    </div>
  </div>

  <div style="clear:both;"></div>

  <!-- results table, pagin etc. -->
  <div id="results" style="display:none;">
    <!-- pagination -->
    <div class="pagination">
      <tiles:insert name="paging.tile">
        <tiles:put name="resultsTable" beanName="pagedResults" />
          <tiles:put name="currentPage" value="bagDetails" />
          <tiles:put name="bag" beanName="bag" />
      </tiles:insert>
    </div>

    <div style="clear:both;"></div>

    <!-- results table -->
    <div class="table">
    <%-- Table displaying bag elements --%>
      <tiles:insert name="resultsTable.tile">
        <tiles:put name="pagedResults" beanName="pagedResults" />
          <tiles:put name="currentPage" value="bagDetails" />
          <tiles:put name="bagName" value="${bag.name}" />
          <tiles:put name="highlightId" value="${highlightId}"/>
      </tiles:insert>
    </div>
  </div>
</div>

<div style="clear:both;"></div>

<div class="bochs wide">
  <div class="inner">
  <h4>Widgets displaying properties of '${bag.name}'</h4>
  <ol class="widgetList">
    <c:forEach items="${widgets}" var="widget">
      <li><a title="toggle widget" href="javascript:toggleWidget('widgetcontainer${widget.id}','togglelink${widget.id}')" id="togglelink${widget.id}" class="active">${widget.title}</a></li>
    </c:forEach>
  </ol>
  </div>
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
<div class="bochs wide">
   <h4>Template results for '${bag.name}' <span>(<a href="javascript:toggleAll(${aspectCount}, '${templateIdPrefix}', 'expand', null, true);">expand all <img src="images/disclosed.gif"/></a> / <a href="javascript:toggleAll(${aspectCount}, '${templateIdPrefix}', 'collapse', null, true);">collapse all <img src="images/undisclosed.gif"/></a>)</span></h4>
  <fmt:message key="bagDetails.templatesHelp">
    <fmt:param>
              <img src="images/disclosed.gif"/> / <img src="images/undisclosed.gif"/>
      </fmt:param>
  </fmt:message>
</div>
<div style="clear:both;"></div>

  <div class="body">

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

<script type="text/javascript" charset="utf-8">
    jQuery(document).ready(function () {
        /*
        jQuery(".tb_button").click(function () {
            toggleToolBarMenu(this);
        });
        */

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
          extraSpace : 15
      });
    });
</script>
