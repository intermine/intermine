<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>

<!-- wikiLink.jsp -->
<%--
to correct an error in the data loaded
--%>


<c:if test="${(!empty object.wikiLink)}">
<c:set var="correctedLink"
value="${fn:replace(object.wikiLink,'http://wiki.modencode.org/project/index.php/','http://wiki.modencode.org/project/index.php?')}"/>

<c:choose>
<c:when test="${fn:containsIgnoreCase(correctedLink,'title')}">
<c:set var="thisValue"
value="${fn:substringAfter(correctedLink, 'http://wiki.modencode.org/project/index.php?title=')}"/>
</c:when>
<c:otherwise>
<c:set var="thisValue"
value="${fn:substringAfter(correctedLink, 'http://wiki.modencode.org/project/index.php?')}"/>
</c:otherwise>
</c:choose>

<ul>
<li class="external">
<html:link href="${correctedLink}" target="new"
   title="Access Wiki information on ${thisValue}">${correctedLink}
</html:link>
</li>
</ul>
<div style="clear:both;"></div>

</c:if>

<!-- /wikiLink.jsp -->
