<%@ tag body-content="scriptless" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>

<%@ attribute name="section" required="true" %>

<!-- ${section} -->
<c:forEach var="res" items="${imf:getHeadResources(section)}">
    <!-- ${res.key} -->
    <c:set var="url" value="${res.url}"/>
    <c:choose>
      <c:when test="${res.type == 'css'}">
        <link rel="stylesheet" type="text/css"
        <c:choose>
            <c:when test="${res.isRelative}">href="<html:rewrite page="${url}"/>"</c:when>
            <c:otherwise>href="${url}"</c:otherwise>
        </c:choose>
        />
      </c:when>
      <c:otherwise>
        <script type="text/javascript"
        <c:choose>
            <c:when test="${res.isRelative}">src="<html:rewrite page="${url}"/>"</c:when>
            <c:otherwise>src="${url}"</c:otherwise>
        </c:choose>
        ></script>
      </c:otherwise>
   </c:choose>
</c:forEach>
