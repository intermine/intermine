<%@ tag body-content="scriptless" %>
<%@ attribute name="text" required="true" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<%--
  Expects a div called ctxHelpDiv that will be shown/hidden and a div
  called ctxHelpText that should surround the help text area.
--%>

<jsp:useBean id="linkParams" scope="page" class="java.util.TreeMap">
  <c:set target="${linkParams}" property="ctxHelpTxt" value="${text}" />
</jsp:useBean>

<c:set var="text" value="${fn:replace(text,'\\\'','&amp;#039;')}"/>
<c:set var="text" value="${fn:replace(text,'\"','&amp;quot;')}"/>


<html:link action="/contextHelp" name="linkParams"
   onclick="document.getElementById('ctxHelpTxt').innerHTML='${text}';document.getElementById('ctxHelpDiv').style.display='';return false"
   title="${text}">
  <jsp:doBody/>
</html:link>
