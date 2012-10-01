<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="/WEB-INF/imutil.tld" prefix="imutil" %>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>

<!-- report.jsp -->
<html:xhtml/>

<script type="text/javascript">
  <%-- apply white background as report page loads slowly and body bg will show through --%>
  var pageBackgroundColor = jQuery('body').css('background-color');
  jQuery('body').css('background-color', '#FFF');
</script>

<c:choose>
  <c:when test="${object != null}">

<script type="text/javascript">
  <%-- the number of entries to show in References & Collections before switching to "show all" --%>
  var numberOfTableRowsToShow = '${object.numberOfTableRowsToShow}'; <%-- required on report.js --%>
  numberOfTableRowsToShow = (numberOfTableRowsToShow == '') ? 30 : parseInt(numberOfTableRowsToShow);
</script>

<link rel="stylesheet" type="text/css" href="css/960gs.css" />
<link rel="stylesheet" type="text/css" href="css/report.print.css" media="print" />

<div id="header_wrap">

  <div id="object_header">
    <c:if test="${object.headerLink != null}">
        <c:set var="headerLink" value="${object.headerLink}"/>
        <div id="headerLink">
            <a href="${headerLink.url}" target="new">
              <c:choose>
                <c:when test="${headerLink.text != null || headerLink.image != null}">
                  <c:if test="${headerLink.image != null}">
                      <img src="model/images/${headerLink.image}" />
                  </c:if>
                  <c:if test="${headerLink.text != null}">
                      ${headerLink.text}
                  </c:if>
                </c:when>
                <c:otherwise>
                  ${headerLink.url}
                </c:otherwise>
              </c:choose>
            </a>
        </div>
    </c:if>
    <a name="summary">
    <h1 class="title">
        <im:displaypath path="${object.type}"/>:
        <c:forEach var="title" varStatus="status" items="${object.titleMain}">
          <c:if test="${status.count > 0}"> </c:if><strong><c:out value="${title.value.formatted}" /></strong>
        </c:forEach>
        <c:forEach var="title" varStatus="status" items="${object.titleSub}">
          <c:if test="${status.count > 0}"> </c:if><c:out value="${title.value.formatted}" />
        </c:forEach>
    </h1>
    </a>

    <%-- summary short fields --%>
    <table class="fields">
      <c:set var="tableCount" value="0" scope="page" />

      <c:forEach var="field" items="${object.objectSummaryFields}">
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

          <c:set var="fieldDisplayText"
            value="${imf:formatFieldChain(field.pathString, INTERMINE_API, WEBCONFIG)}"/>
          <c:choose>
            <c:when test="${field.valueHasDisplayer}">
              <td class="label">
                  ${fieldDisplayText}&nbsp;
                  <im:typehelp type="${field.pathString}"/>
              </td>
              <td><strong>
                <!-- pass value to displayer -->
                <c:set var="interMineObject" value="${object.object}" scope="request"/>
                  <tiles:insert page="${field.displayerPage}">
                    <tiles:put name="expr" value="${field.name}" />
                  </tiles:insert>
              </strong></td>
              <c:set var="tableCount" value="${tableCount+1}" scope="page" />
            </c:when>
            <c:otherwise>
              <c:if test="${!field.doNotTruncate && !empty field.value}">
                <td class="label">${fieldDisplayText}&nbsp;<im:typehelp type="${field.pathString}"/></td>
                <td><strong><c:out value="${field.value}" /></strong></td>
                <c:set var="tableCount" value="${tableCount+1}" scope="page" />
              </c:if>
            </c:otherwise>
          </c:choose>
      </c:forEach>
    </table>

    <%-- summary long fields --%>
    <table>
      <c:forEach var="field" items="${object.objectSummaryFields}">
        <c:if test="${field.doNotTruncate}">
          <tr>
            <c:if test="${!empty field.value}">
              <td class="label">${field.name}&nbsp;<im:typehelp type="${field.pathString}"/></td>
              <td><strong><c:out value="${field.value}" /></strong></td>
            </c:if>
          </tr>
        </c:if>
      </c:forEach>
    </table>

    <%-- header Inline Lists --%>
    <c:if test="${object.hasHeaderInlineLists}">
      <tiles:insert page="/reportHeaderInlineLists.jsp">
        <tiles:put name="object" beanName="object" />
      </tiles:insert>
    </c:if>

  <%-- shown @ top displayers --%>
  <div class="displayers">
    <tiles:insert page="/reportDisplayers.jsp">
      <tiles:put name="placement" value="top" />
      <tiles:put name="reportObject" beanName="object" />
    </tiles:insert>
  </div>

    <%-- permalink --%>
    <%-- <p class="share">Share this page: <a href="${stableLink}">${stableLink}</a></p> --%>
    <div id="share">
      <a></a>
      <div class="popup">
        <span class="close"></span>
        Paste the following link
        <input type="text" value="${stableLink}">
      </div>
      <script type="text/javascript">
        //<![CDATA[
        jQuery('#object_header #share a').click(function() {
          // show
          jQuery("#object_header #share div.popup").show();
          // select
          jQuery("#object_header #share div.popup").find('input').select();
        });
        jQuery('#object_header #share div.popup span.close').click(function() {
          // hide
          jQuery("#object_header #share div.popup").hide();
        });
        //]]>
      </script>
  </div>

  </div>
</div>

<div id="content">

<c:if test="${categories != null}">
  <div id="menu-target">&nbsp;</div>
  <div id="toc-menu-wrap">
    <tiles:insert name="reportMenu.jsp">
      <tiles:put name="summary" value="current" />
    </tiles:insert>
  </div>
  <div id="fixed-menu">
    <tiles:insert name="reportMenu.jsp" />
  </div>
  <script type="text/javascript">
    //<![CDATA[
    (function() {
      jQuery('#fixed-menu').hide(); // hide for IE7
      jQuery(window).scroll(function() {
        // transition fix
        if (jQuery('#menu-target').isInView('partial')) {
          jQuery('#fixed-menu').hide();
        } else {
          jQuery('#fixed-menu').show();
        }
        // where are we
        var currentAspect = null;
        var currentAspectDistance = 9999;

        // distance from top (screen)
        var screenTop = jQuery(window).scrollTop();
        // distance from bottom (screen)
        var screenBottom = screenTop + jQuery(window).height();
        // center of the screen
        var screenMiddle = ((screenBottom - screenTop) / 2) + screenTop;

        // traverse aspect blocks
        jQuery('div.aspectBlock').each(function(i) {
          // is this aspect in view?
          if (jQuery(this).isInView('partial')) {
              // top & bottom distance for the element, increase the top one as divs align
              var elementTop = jQuery(this).offset().top + 1;
              var elementBottom = elementTop + jQuery(this).height() - 1;

              // absolute distance from the middle of the screen is...
              var elementTopDistance = Math.abs(elementTop - screenMiddle);
              var elementBottomDistance = Math.abs(elementBottom - screenMiddle);

              // save the one that is closer to the middle
              if (elementTopDistance < currentAspectDistance) {
                currentAspectDistance = elementTopDistance;
                currentAspect = jQuery(this).attr('id');
              }
              if (elementBottomDistance < currentAspectDistance) {
                currentAspectDistance = elementBottomDistance;
                currentAspect = jQuery(this).attr('id');
              }
          }
        });

        if (currentAspect != null) {
          // strip the 'Category' suffix
          currentAspect = currentAspect.substring(0, currentAspect.length - 8);
          // find the one link in the top menu that corresponds to where we are
          jQuery('#fixed-menu div.links a').each(function(i) {
              if (jQuery(this).text() == currentAspect) {
                  jQuery(this).addClass('current');
              } else {
                  jQuery(this).removeClass('current');
              }
          });
        }

      });

      if (jQuery(window).width() < '900') {
        jQuery('div.wrap').each(function(index) {
            jQuery(this).addClass('smallscreen');
        });
      }
    })();
  //]]>
  </script>
</c:if>

<div class="container_12">

 <c:set value="${fn:length(CATEGORIES)}" var="aspectCount" /> <c:set
  var="templateIdPrefix" value="reportTemplate${objectType}" /> <c:set
  var="miscId" value="reportMisc${objectType}" /> <%-- All other references and collections --%>

<script type="text/javascript">
  //<![CDATA[
    var modifyDetailsURL = '<html:rewrite action="/modifyDetails"/>';
    var detailsType = 'object';
  //]]>-->
</script>
<script type="text/javascript" src="js/inlinetemplate.js"></script>

<div style="float:right;" class="grid_2 sidebar">
  <div id="in-lists">
    <tiles:insert name="reportInList.tile">
      <tiles:put name="object" beanName="object"/>
    </tiles:insert>
  </div>

  <c:set var="object_bk" value="${object}"/>
  <c:set var="object" value="${reportObject.object}" scope="request"/>
  <div id="external-links">
    <tiles:insert name="otherMinesLink.tile" />
    <tiles:insert name="attributeLinks.tile" >
        <tiles:put name="reportObject" beanName="object" />
  </tiles:insert>
  </div>
  <c:set var="object" value="${object_bk}"/>

  <%-- shown in a sidebar displayers --%>
  <div id="displayers" class="table">
    <tiles:insert page="/reportDisplayers.jsp">
      <tiles:put name="placement" value="sidebar" />
      <tiles:put name="reportObject" beanName="object" />
    </tiles:insert>
  </div>
</div>

<div class="grid_10">

  <div id="summaryCategory" class="aspectBlock">
   <tiles:insert page="/reportDisplayers.jsp">
      <tiles:put name="placement" value="summary" />
    <tiles:put name="reportObject" beanName="object" />
     </tiles:insert>

   <tiles:insert name="reportAspect.tile">
        <tiles:put name="mapOfInlineLists" beanName="mapOfInlineLists" />
        <tiles:put name="placement" value="im:summary" />
        <tiles:put name="reportObject" beanName="object" />
        <tiles:put name="trail" value="${request.trail}" />
        <tiles:put name="aspectId" value="${templateIdPrefix}${status.index}" />
        <tiles:put name="opened" value="${status.index == 0}" />
     </tiles:insert>
  </div>

  <c:forEach items="${categories}" var="aspect" varStatus="status">
    <div id="${fn:replace(aspect, " ", "_")}Category" class="aspectBlock">
      <tiles:insert name="reportAspect.tile">
        <tiles:put name="mapOfInlineLists" beanName="mapOfInlineLists" />
        <tiles:put name="placement" value="im:aspect:${aspect}" />
        <tiles:put name="reportObject" beanName="object" />
        <tiles:put name="trail" value="${request.trail}" />
        <tiles:put name="aspectId" value="${templateIdPrefix}${status.index}" />
        <tiles:put name="opened" value="${status.index == 0}" />
      </tiles:insert>
  </div>
  </c:forEach>

  <div id="OtherCategory" class="aspectBlock">
    <c:if test="${categories != null}">
      <c:if test="${fn:length(placementRefsAndCollections['im:aspect:Miscellaneous']) > 0 || fn:length(listOfUnplacedInlineLists) > 0}">
        <div class="clear">&nbsp;</div>
        <a name="other"><h2>Other</h2></a>
      </c:if>
    </c:if>

    <tiles:insert page="/reportUnplacedInlineLists.jsp">
      <tiles:put name="listOfUnplacedInlineLists" beanName="listOfUnplacedInlineLists" />
    </tiles:insert>

    <tiles:insert page="/reportRefsCols.jsp">
      <tiles:put name="object" beanName="object" />
      <tiles:put name="placement" value="im:aspect:Miscellaneous" />
    </tiles:insert>
  </div>
</div>
</div>
</div>

  </c:when>
  <c:otherwise>
    <script type="text/javascript">
        <%-- fudge the layout I can? --%>
        jQuery("#pagecontentmax").attr('id', "pagecontent");
    </script>
    <div id="wrap">
      <h1>Object not found</h1>
      <p>That which you were looking for does not exist. Try...
        <ol>
          <li>going to the <a href="/">home page</a></li>
          <li>using the <a href="/keywordSearchResults.do">quicksearch</a></li>
          <li>or <a onclick="showContactForm()">Contact us</a> at support [at] flymine.org</li>
        </ol>
      </p>
    </div>
  </c:otherwise>
</c:choose>

<script type="text/javascript">
  jQuery('body').css('background-color', pageBackgroundColor);
</script>
