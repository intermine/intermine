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

<html:html locale="true" xhtml="true">

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

  <div id="pagecontentcontainer" align="center">
    <c:choose>
    <c:when test="${!empty fixedLayout}">
      <div id="pagecontent">
    </c:when>
    <c:otherwise>
      <div id="pagecontentmax">
    </c:otherwise>
    </c:choose>

  <div id="navtrail">

  <!-- contact us -->

    <p id="contactUsLink" style="display:none;" class="alignleft">
    <a href="#" onclick="showContactForm();return false;"><fmt:message key="feedback.link"/></a>
    </p>


    <!-- Nav trail -->
  <fmt:message key="${pageName}.tab" var="tab" />
  <c:if test="${tab != '???.tab???' && tab != '???tip.tab???'}">
    <p class="alignright">
    <html:link href="${WEB_PROPERTIES['project.sitePrefix']}"><c:out value="${WEB_PROPERTIES['project.title']}" escapeXml="false"/></html:link>
    <c:if test="${! empty tab }">
      &nbsp;&gt;&nbsp;<html:link action="${tab}"><fmt:message key="menu.${tab}" /></html:link>
      <c:if test="${pageName != tab}">
        <fmt:message key="${pageName}.title" var="pageTitle">
          <fmt:param value="${param.name}"/>
        </fmt:message>
        &nbsp;&gt;&nbsp;<c:out value="${pageTitle}" />
      </c:if>
    </c:if>
    <im:contextHelp/>
</p>
  </c:if>
 </div>

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
            document.write(unescape("%3Cscript src='http://www.google-analytics.com/ga.js' type='text/javascript'%3E%3C/script%3E"));
        </script>
        <script type="text/javascript">
            var pageTracker = _gat._getTracker('${googleAnalyticsId}');
            pageTracker._initData();
            pageTracker._trackPageview();
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

