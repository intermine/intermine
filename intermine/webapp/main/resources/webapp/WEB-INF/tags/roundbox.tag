<%@ tag body-content="scriptless"  %>
<%@ attribute name="title" required="false" %>
<%@ attribute name="titleKey" required="false" %>
<%@ attribute name="stylename" required="true" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%
String stylename = (String) jspContext.getAttribute("stylename");
if(new java.io.File(application.getRealPath("model")+"/"+stylename+"_L.gif").exists()) {
 	request.setAttribute("useImages","true");
}
%>

<c:if test="${!empty titleKey}">
  <fmt:message key="${titleKey}" var="title"/>
</c:if>

<c:if test="${!empty title}">
 <div class="roundcornerbox" id="${stylename}" >
    <h1><c:out value="${title}"/></h1>
    <jsp:doBody/>
 </div>
</c:if>
