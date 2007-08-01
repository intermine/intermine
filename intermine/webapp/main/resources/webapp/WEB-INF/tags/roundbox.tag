<%@ tag body-content="scriptless"  %>
<%@ attribute name="title" required="false" %>
<%@ attribute name="titleKey" required="false" %>
<%@ attribute name="height" required="false" %>
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
	<c:if test="${useImages != 'true'}"><c:set var="tableStyle" value="${tableStyle};border: 1px solid #888;" /></c:if>
	<div class="roundcornerbox" >
	<c:choose>
	<c:when test="${useImages}">
	  <h1><c:out value="${title}"/></h1>
	</c:when>
	</c:choose>
	<c:if test="${useImages}"><c:set var="bodyStyle" value="boxcontent${stylename}"/></c:if>
      <jsp:doBody/>
	</div>
</c:if>
