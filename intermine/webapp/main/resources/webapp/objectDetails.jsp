<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil"%>

<!-- KEEP THIS! ${object.fieldExprs} -->

<!-- objectDetails.jsp -->
<html:xhtml />

<link rel="stylesheet" type="text/css" href="css/960gs.css" />

<div id="header_wrap">

<div id="object_header"><tiles:get name="objectTrail.tile" />
<a name="summary"></a><h1 class="title">
    ${object.objectType}: <strong>${object.titleMain}</strong> ${object.titleSub}</h1></a>

<table class="fields">
  <c:set var="tableCount" value="0" scope="page" />

  <%-- show in summary fields --%>
  <c:forEach items="${object.fieldExprs}" var="expr">
    <c:choose>
      <c:when test="${object.fieldConfigMap[expr].showInSummary}">

        <c:if test="${tableCount %2 == 0}">
          <c:choose>
            <c:when test="${tableCount == 0}">
              <tr>
            </c:when>
            <c:otherwise>
              </tr><tr>
            </c:otherwise>
          </c:choose>
        </c:if>
        <c:choose>
          <c:when test="${!empty object.fieldConfigMap[expr].displayer}">
            <c:set var="interMineObject" value="${object.object}"
              scope="request" />
              <td>${expr}:</td>
              <td><strong> <tiles:insert
                page="${object.fieldConfigMap[expr].displayer}">
                <tiles:put name="expr" value="${expr}" />
              </tiles:insert> </strong></td>
              <c:set var="tableCount" value="${tableCount+1}" scope="page" />
          </c:when>
          <c:otherwise>
            <c:if test="${!empty object.fieldValues[expr] && !object.fieldConfigMap[expr].doNotTruncate}">
              <td>${expr}:</td>
              <td><strong>${object.fieldValues[expr]}</strong></td>
              <c:set var="tableCount" value="${tableCount+1}" scope="page" />
            </c:if>
          </c:otherwise>
        </c:choose>
      </c:when>
    </c:choose>
  </c:forEach>

  <%-- all other fields --%>
  <c:forEach items="${object.attributes}" var="entry">
    <c:if test="${!object.fieldConfigMap[entry.key].showInSummary && !object.fieldConfigMap[entry.key].sectionOnRight}">

      <c:if test="${tableCount %2 == 0}">
        <c:choose>
          <c:when test="${tableCount == 0}">
            <tr>
          </c:when>
          <c:otherwise>
            </tr><tr>
          </c:otherwise>
        </c:choose>
      </c:if>

      <c:choose>
        <c:when test="${object.longAttributes[entry.key] != null && object.longAttributesTruncated[entry.key] == null}">
          <td>${entry.key}</td>
          <td>${object.longAttributes[entry.key]}</td>
          <c:set var="tableCount" value="${tableCount+1}" scope="page" />
        </c:when>
        <c:otherwise>
          <td>${entry.key}</td>
          <td><strong><im:value>${entry.value}</im:value></strong></td>
          <c:set var="tableCount" value="${tableCount+1}" scope="page" />
        </c:otherwise>
      </c:choose>

    </c:if>
  </c:forEach>
</table>

<table>
<c:forEach items="${object.fieldExprs}" var="expr">
  <c:if test="${!empty object.fieldValues[expr] && object.fieldConfigMap[expr].doNotTruncate}">
    <tr>
      <td>${expr}:</td>
      <td><strong>${object.fieldValues[expr]}</strong></td>
    </tr>
  </c:if>
</c:forEach>
  <c:forEach items="${object.attributes}" var="entry">
    <c:if test="${!object.fieldConfigMap[entry.key].showInSummary && !object.fieldConfigMap[entry.key].sectionOnRight}">
      <c:if test="${object.longAttributes[entry.key] != null && object.longAttributesTruncated[entry.key] != null}">
        <tr>
          <td>${entry.key}</td>
          <td><im:value>${entry.value}</im:value></td>
        </tr>
      </c:if>
    </c:if>
  </c:forEach>
</table>

<c:if test="${object.hasHeaderInlineLists}">
  <div class="box">
    <tiles:insert page="/objectDetailsHeaderInlineLists.jsp">
      <tiles:put name="object" beanName="object" />
    </tiles:insert>
  </div>
</c:if>

</div>
</div>

<div id="content">

<c:if test="${categories != null}">
  <div id="menu-target">&nbsp;</div>
  <div id="toc-menu-wrap">
    <tiles:insert name="objectDetailsMenu.jsp" />
  </div>
  <div id="fixed-menu">
    <tiles:insert name="objectDetailsMenu.jsp" />
  </div>
  <script type="text/javascript">
  jQuery('#fixed-menu').hide(); // hide for IE7
  jQuery(window).scroll(function() {
    if (jQuery('#menu-target').isInView('partial')) {
      jQuery('#fixed-menu').hide();
    } else {
      jQuery('#fixed-menu').show();
    }
  });
  </script>
</c:if>

<div class="container_12">

<%--
<p class="description grid_12"><img
  src="model/images/report_page/question-mark.png" alt="info">Summary
represents a lorem ipsum dolor sit amet nunc eros felis, porta quis
hendrerit sit amet, convallis in ipsum. Cras congue, nisi non volutpat
scelerisque, augue nibh posuere dui, at tristique augue ante at velit.
Vivamus arcu eros, tristique eu imperdiet a, mollis sed dui. Ut blandit,
arcu non condimentum porta, quam lacus porttitor eros.</p>

<div class="clear">&nbsp;</div>

    <div class="box grid_8">
        <h3>Quick Links</h3>
        <div class="inner">
            <ul id="quick_links" class="quick_links">
                <!-- <li><a href="#"><span class="left">&nbsp;</span><strong>Graphs</strong><span class="right">&nbsp;</span></a></li> -->
            </ul>
            <script type="text/javascript">
              jQuery(document).ready(function() {
                jQuery("div.loadOnScroll").each(function(index) {
                    // fetch the text from the title
                    var text = jQuery(this).find('h3').text();
                    // split it on a space after removing unwanted whitespace
                    var textArray = text.replace(/^\s*|\s*$/g,'').split(" ");
                    // fetch the amount as the first value
                    var amount = textArray[0];
                    var textTitle = '';
                    // traverse the rest of the text for the title
                    for (i = 1; i < textArray.length; i++) {
                        textTitle += textArray[i];
                    }
                    // finally create an anchor
                    var anchor = '/${WEB_PROPERTIES['webapp.path']}/objectDetails.do?<c:out value="${pageContext.request.queryString}" />'
                      + '#' + jQuery(this).find('h3').find('a').attr('name');

                    // create the list item
                    jQuery("#quick_links").append('<li><a href="' + anchor + '"><span class="left">&nbsp;</span>\
                          <strong>' + textTitle + '</strong>' + amount + '<span class="right">&nbsp;</span></a></li>')
                });
              });
            </script>
        </div>
    </div>
    --%> <c:set value="${fn:length(CATEGORIES)}" var="aspectCount" /> <c:set
  var="templateIdPrefix" value="objectDetailsTemplate${objectType}" /> <c:set
  var="miscId" value="objectDetailsMisc${objectType}" /> <%-- All other references and collections --%>
<script type="text/javascript">
      <!--//<![CDATA[
        var modifyDetailsURL = '<html:rewrite action="/modifyDetails"/>';
        var detailsType = 'object';
      //]]>-->
      </script> <script type="text/javascript" src="js/inlinetemplate.js"></script>

<div style="float:right;" class="box grid_3">
  <div id="in-lists">
    <tiles:insert name="objectDetailsInList.tile">
      <tiles:put name="object" beanName="object"/>
    </tiles:insert>
  </div>
  <h3>Other Mines</h3>
  <ul>
    <li><a href="#">RatMine</a></li>
    <li><a href="#">YeastMine</a></li>
    <li><a href="#">ZebraMine</a></li>
  </ul>
  <br />
  <c:set var="object_bk" value="${object}"/>
  <c:set var="object" value="${displayObject.object}" scope="request"/>
  <tiles:insert name="attributeLinks.tile" />
  <c:set var="object" value="${object_bk}"/>
</div>

<div class="box grid_9">

  <tiles:insert page="/objectDetailsCustomDisplayers.jsp">
    <tiles:put name="placement" value="summary" />
    <tiles:put name="displayObject" beanName="object" />
  </tiles:insert>

  <tiles:insert
    page="/objectDetailsDisplayers.jsp">
    <tiles:put name="placement" value="" />
    <tiles:put name="displayObject" beanName="object" />
    <tiles:put name="heading" value="true" />
  </tiles:insert>

  <c:forEach items="${categories}" var="aspect" varStatus="status">
    <tiles:insert name="objectDetailsAspect.tile">
    <tiles:put name="mapOfInlineLists" beanName="mapOfInlineLists" />
    <tiles:put name="placement" value="im:aspect:${aspect}" />
    <tiles:put name="displayObject" beanName="object" />
    <tiles:put name="trail" value="${request.trail}" />
    <tiles:put name="aspectId" value="${templateIdPrefix}${status.index}" />
    <tiles:put name="opened" value="${status.index == 0}" />
  </tiles:insert>
  </c:forEach>

  <c:if test="${categories != null}">
    <c:if test="${fn:length(placementRefsAndCollections['im:aspect:Miscellaneous']) > 0 || fn:length(listOfUnplacedInlineLists) > 0}">
      <div class="clear">&nbsp;</div>
      <a name="other"><h2>Other</h2></a>
    </c:if>
  </c:if>
  <tiles:insert page="/objectDetailsUnplacedInlineLists.jsp">
    <tiles:put name="listOfUnplacedInlineLists" beanName="listOfUnplacedInlineLists" />
  </tiles:insert>

  <tiles:insert page="/objectDetailsRefsCols.jsp">
    <tiles:put name="object" beanName="object" />
    <tiles:put name="placement" value="im:aspect:Miscellaneous" />
  </tiles:insert>
</div>

</div>

</div>

<!-- /objectDetails.jsp -->