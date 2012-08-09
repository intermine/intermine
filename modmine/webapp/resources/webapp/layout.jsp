<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-1-transitional.dtd">

<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- layout.jsp -->
<html:xhtml/>

<html:html lang="true" xhtml="true">

<c:set var="iePre7" value='<%= new Boolean(request.getHeader("user-agent").matches(".*MSIE [123456].*")) %>' scope="request"/>

  <tiles:importAttribute name="pageName" scope="request"/>

  <head>

<!-- for google webmaster -->
<meta name="google-site-verification" content="${WEB_PROPERTIES['searchengines.google']}" />

<!-- for yahoo -->
<META name="y_key" content="${WEB_PROPERTIES['searchengines.yahoo']}" />

<!-- for microsoft -->
<meta name="msvalidate.01" content="${WEB_PROPERTIES['searchengines.msn']}" />

<html:base/>

<fmt:message key="${pageName}.noFollow" var="noFollow" />

  <c:if test="${noFollow == 'true'}">
      <META NAME="ROBOTS" CONTENT="NOFOLLOW"/>
  </c:if>

  <fmt:message key="${pageName}.title" var="pageNameTitle"/>

    <tiles:insert name="htmlHead.tile">
      <tiles:put name="bagName" value="${param.bagName}"/>
      <tiles:put name="objectId" value="${param.id}"/>
      <tiles:put name="name" value="${param.name}"/>
      <tiles:put name="pageName" value="${pageName}"/>
      <tiles:put name="pageNameTitle" value="${pageNameTitle}"/>
      <tiles:put name="scope" value="${scope}"/>
    </tiles:insert>
  </head>
  <body>

  <!-- Check if the current page has fixed layout -->
  <c:forTokens items="${WEB_PROPERTIES['layout.fixed']}" delims="," var="currentPage">
    <c:if test="${pageName == currentPage}">
      <c:set var="fixedLayout" value="true" />
    </c:if>
  </c:forTokens>

  <!-- Page header -->
  <tiles:insert name="headMenu.tile">
    <tiles:put name="fixedLayout" value="${fixedLayout}"/>
  </tiles:insert>

  <div id="pagecontentcontainer" align="center" class="${pageName}${subtabs[subtabName]}-page">
    <c:choose>
    <c:when test="${!empty fixedLayout}">
      <div id="pagecontent">
    </c:when>
    <c:otherwise>
      <div id="pagecontentmax">
    </c:otherwise>
    </c:choose>

<%-- contextual help is not working...
  <div id="navtrail">
    <p id="contactUsLink" style="display:none;" class="alignleft">
    <a href="#" onclick="showContactForm();return false;"><fmt:message key="feedback.link"/></a>
    </p>

  <c:if test="${pageName != 'report'}">
  <fmt:message key="${pageName}.tab" var="tab" />
    <c:if test="${tab != '???.tab???' && tab != '???tip.tab???'}">
        <p class="alignright"><im:contextHelp/></p>
    </c:if>
  </c:if>
 </div>
--%>

<div style="clear: both;"></div>

      <%-- Render messages --%>
      <tiles:get name="errorMessagesContainers"/>

<%-- HERE TOP MESSAGES
<div class="topBar messages">
</div>
--%>

      <%-- Context help bar --%>
      <tiles:insert page="/contextHelp.jsp"/>

      <%-- Display page specific hints if available --%>
      <tiles:insert name="hints.tile">
        <tiles:put name="pageName" value="${pageName}"/>
      </tiles:insert>

      <tiles:get name="body"/>

      <%-- Render messages --%>
      <tiles:get name="errorMessages"/>

      <%-- footer (welcome logo, bottom nav, and feedback link) --%>
    <c:import url="footer.jsp"/>

      <c:if test="${param.debug != null}">
        <im:vspacer height="11"/>
          <tiles:insert page="/session.jsp"/>
      </c:if>

    <c:if test="${IS_SUPERUSER}">
      <div class="admin-msg">
        <span class="smallnote">
          <fmt:message key="intermine.superuser.msg"/>
        </span>
      </div>
    </c:if>

    <c:set var="googleAnalyticsId" value="${WEB_PROPERTIES['google.analytics.id']}"/>
    <c:if test="${!empty googleAnalyticsId}">
        <script type="text/javascript">
          switch ("${userTracking}") {
            case "1":
            
              var _gaq = _gaq || [];
              _gaq.push(['_setAccount', '${googleAnalyticsId}']);
              _gaq.push(['_trackPageview']);

              (function() {
                var ga = document.createElement('script');
                ga.type = 'text/javascript';
                ga.async = true;
                ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
                var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
              })();

              break;

            case "2":
              // save cookie
              var saveTrackingAnswer = function(answer) {
                var date = new Date();
                date.setTime(date.getTime() + 31536000000);
                document.cookie = "userTracking=" + escape(answer) + "; expires=" + date.toUTCString() + "; path=/";
              };
              // show msg
              jQuery("#ctxHelpDiv").after( function() {
                return el = jQuery("<div/>", { 'class': 'topBar info userTracking', 'style': 'margin:10px 24px' })
                .html( function() {
                    return jQuery('<p/>', { 'text': '${userTrackingMessage}' })
                    .append( function() {
                      return jQuery('<a/>', {
                        'text': 'No',
                        'href': '#',
                        'click': function(e) {
                          e.preventDefault();
                          saveTrackingAnswer("0");
                          jQuery(el).remove();
                        }
                      })
                    } )
                    .append( function() {
                      return jQuery('<a/>', {
                        'text': 'Yes',
                        'href': '#',
                        'style': 'margin:0 10px',
                        'click': function(e) {
                          e.preventDefault();
                          saveTrackingAnswer("1");
                          jQuery(el).remove();
                        }
                      })
                    } )
                } )
              } );
          }
        </script>
    </c:if>
    <script type="text/javascript">
    jQuery(document).ready(function() {
        display("contactUsLink", true);
    });
    </script>
    <c:if test="${!empty fixedLayout}">
      </div>
    </c:if>
  </div>
</body>
</html:html>
<!-- /layout.jsp -->

