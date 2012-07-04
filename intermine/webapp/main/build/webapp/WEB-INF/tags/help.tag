<%@ tag body-content="scriptless" %>
<%@ attribute name="text" required="false" %>
<%@ attribute name="key" required="false" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%--
  Turns the body into a link which, when clicked, shows the user some
  context help.

  Expects a div called ctxHelpDiv that will be shown/hidden and a div
  called ctxHelpText that should surround the help text area.
--%>

<c:if test="${!empty key}">
  <fmt:message var="text" key="${key}"/>
</c:if>

<jsp:useBean id="linkParams" scope="page" class="java.util.TreeMap">
  <c:set target="${linkParams}" property="ctxHelpTxt" value="${text}" />
</jsp:useBean>

<c:set var="origText" value="${text}"/>

<%-- crazy escaping follows --%>
<c:set var="text" value="${fn:replace(text,'\\\'','&amp;#039;')}"/>
<c:set var="text" value="${fn:replace(text,'\"','&amp;quot;')}"/>

<html:link action="/contextHelp" name="linkParams"
   onclick="document.getElementById('ctxHelpTxt').innerHTML='${text}';document.getElementById('ctxHelpDiv').style.display='';window.scrollTo(0, 0);return false"
   title="${origText}">
  <jsp:doBody/>
</html:link>
