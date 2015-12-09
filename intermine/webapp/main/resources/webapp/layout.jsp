<!DOCTYPE html>

<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>

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

<c:if test="${pageName == 'begin'}">
  <html:base/>
  <%-- <base href="http://${pageContext.request.serverName}:${pageContext.request.serverPort}${pageContext.request.contextPath}/${pageName}.do" /> --%>
</c:if>

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

    <link rel="shortcut icon" type="image/x-icon" href="model/images/favicon.ico">

  </head>

  <!-- Check if the current page has fixed layout -->
  <c:forTokens items="${WEB_PROPERTIES['layout.fixed']}" delims="," var="currentPage">
    <c:if test="${pageName == currentPage}">
      <c:set var="fixedLayout" value="true" />
    </c:if>
  </c:forTokens>

  <c:choose>
    <c:when test="${!empty fixedLayout}">
      <body class="fixed">
    </c:when>
    <c:otherwise>
      <body class="stretched">
    </c:otherwise>
  </c:choose>

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

<div style="clear: both;"></div>

      <%-- Render messages --%>
      <tiles:get name="errorMessagesContainers"/>

      <%-- Context help bar --%>
      <tiles:insert page="/contextHelp.jsp"/>

      <%-- Display page specific hints if available --%>
      <tiles:insert name="hints.tile">
        <tiles:put name="pageName" value="${pageName}"/>
      </tiles:insert>

      <tiles:get name="body"/>

<script type="text/javascript">
jQuery(document).ready(function() {
  jQuery("p#contactUsLink").toggle();
  });

setOption(['CDN'], 'server', "${WEB_PROPERTIES['head.cdn.location']}");

if ((typeof intermine != 'undefined') && (intermine.Service != null)) {
    // Set up the service, if required.
    var root = window.location.origin + "/${WEB_PROPERTIES['webapp.path']}";

    $SERVICE = new intermine.Service({
        "root": root,
        "token": "${PROFILE.dayToken}",
        "help": "${WEB_PROPERTIES['feedback.destination']}"
    });

    var notification = new FailureNotification({message: $SERVICE.root + " is incorrect"});

    $SERVICE.fetchVersion().then(reportVersion, notification.render);
    if (typeof imtables !== 'undefined') {
        console.debug('Using imtables: ' + (imtables.version || 'UNKNOWN'));
    }

    // Load list widgets.
    (function() {
      if (window['list-widgets'] != null) {
        // Make sure we have all deps required in `global.web.properties`, otherwise we fail!!!
        var ListWidgets = require('list-widgets');
        window.widgets = new ListWidgets({ 'root': $SERVICE.root, 'token': $SERVICE.token });
      }
    })();

    var ua = jQuery.browser; // kinda evil, but best way to do this for now
    if (ua && ua.msie && parseInt(ua.version, 10) < 9) { // removed in 1.9.1
        new Notification({message: '<fmt:message key="old.browser"/>'}).render();
    }
}

function setOption (ns, key, val) {
    var i, step, options = (window.intermine && window.intermine.options);
    if (!options) return;
    for (i = 0; i < ns.length; i++) {
        step = ns[i];
        options = options[step] || (options[step] = {});
    }
    options[key] = val;
}

function reportVersion (v) {
    console.log("Webservice is at version " + v);
}

$MODEL_TRANSLATION_TABLE = {
    <c:forEach var="cd" items="${INTERMINE_API.model.classDescriptors}" varStatus="cdStat">
        "${cd.unqualifiedName}": {
            displayName: "${imf:formatPathStr(cd.unqualifiedName, INTERMINE_API, WEBCONFIG)}",
            fields: {
                <c:forEach var="fd" items="${cd.allFieldDescriptors}" varStatus="fdStat">
                    <c:set var="fdPath" value="${cd.unqualifiedName}.${fd.name}"/>
                    "${fd.name}": "${imf:formatFieldStr(fdPath, INTERMINE_API, WEBCONFIG)}"<c:if test="${!fdStat.last}">,</c:if>
                </c:forEach>
            }
        }<c:if test="${!cdStat.last}">,</c:if>
    </c:forEach>
};

<c:if test="${! empty WEB_PROPERTIES['constraint.default.value']}">
if (typeof imtables != 'undefined' && imtables.setMessages) {
    imtables.setMessages({
        "conbuilder.ValuePlaceholder": "${WEB_PROPERTIES['constraint.default.value']}",
        "conbuilder.ExtraPlaceholder": "${WEB_PROPERTIES['constraint.default.extra-value']}"
    });
}
</c:if>

</script>

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
                return el = jQuery("<div/>", { 'class': 'topBar info userTracking' })
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
    <c:if test="${!empty fixedLayout}">
      </div>
    </c:if>
  </div>
</body>
</html:html>
<!-- /layout.jsp -->
