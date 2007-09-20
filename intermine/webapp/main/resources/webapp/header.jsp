<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!-- header.jsp -->
<c:set value="${WEB_PROPERTIES['header.links']}" var="headerLinks"/>
<c:if test="${fn:length(headerLinks) > 0}">
<div id="topnav">
	<c:forEach var="entry" items="${headerLinks}" varStatus="status">
		<c:if test="${status.count != 1}">&nbsp;|&nbsp;</c:if><a href="${WEB_PROPERTIES['project.sitePrefix']}/${entry}.shtml">${entry}</a>
	</c:forEach>  
</div>
</c:if>

<div id="loginbar">
    <c:if test="${!empty PROFILE.username}">
        ${PROFILE.username}&nbsp;|&nbsp;
    </c:if>
    <im:login/>
</div>
<div id="title">
   <span id="logo"><im:useTransparentImage src="/model/images/logo.png" id="heading_logo" link="begin.do" width="68px" height="65px" /></span>
   <h1><html:link href="${WEB_PROPERTIES['project.sitePrefix']}/"><c:out value="${WEB_PROPERTIES['project.title']}" escapeXml="false"/></html:link>
   <span class="version"><fmt:message key="header.version"/> <c:out value="${WEB_PROPERTIES['project.releaseVersion']}" escapeXml="false"/></span></h1>
    <p>
      <c:out value="${WEB_PROPERTIES['project.subTitle']}" escapeXml="false"/>
    </p>
</div>
<!-- /header.jsp -->
