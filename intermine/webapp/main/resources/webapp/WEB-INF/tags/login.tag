<%@ tag body-content="scriptless" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@ attribute name="loginMessage" required="false" %>

<% 
String returnToPath = "/" + (String) request.getAttribute("pageName") + ".do";
if (returnToPath != null) {
    if (request.getQueryString() != null) {
        returnToPath += "?" + request.getQueryString();
    }
    String encodedReturnToPath = java.net.URLEncoder.encode(returnToPath); 
    request.setAttribute("returnToPath", encodedReturnToPath);
}
%>

<c:set var="returnToString" value=""/>
<c:if test="${!empty returnToPath && pageName != 'login'}">
  <c:set var="returnToString" value="?returnto=${returnToPath}"/>
</c:if>
<c:choose>
  <c:when test="${!PROFILE.loggedIn}">  
    <a href="/${WEB_PROPERTIES['webapp.path']}/login.do${returnToString}" rel="NOFOLLOW">
      <c:if test="${empty loginMessage}">
        <fmt:message var="loginMessage" key="menu.login"/>
      </c:if>
      ${loginMessage}
    </a>
  </c:when>
  <c:otherwise>
  <a href="<html:rewrite page="/logout.do"/>" rel="NOFOLLOW">
      <fmt:message key="menu.logout"/>
    </a>
  </c:otherwise>
</c:choose>
