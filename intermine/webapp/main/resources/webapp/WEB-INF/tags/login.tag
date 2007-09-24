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
  <c:when test="${!empty PROFILE_MANAGER && empty PROFILE.username}">
    <html:link action="/login.do${returnToString}">
      <c:if test="${empty loginMessage}">
        <fmt:message var="loginMessage" key="menu.login"/>
      </c:if>
      ${loginMessage}
    </html:link>
  </c:when>
  <c:otherwise>
    <html:link action="/logout.do">
      <fmt:message key="menu.logout"/>
    </html:link>
  </c:otherwise>
</c:choose>
