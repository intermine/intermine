<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>

<!-- dataAttribute.jsp -->

<tiles:importAttribute />

<html:xhtml />


<div class="body">

<c:set var="count" value="" />

<c:forEach items="${object.dataAttributes}" var="att" varStatus="a_status">
<c:if test="${a_status.last}">
<c:set var="count" value="${a_status.count}" />
</c:if>
</c:forEach>

<%--========== --%>
<div id="im_aspect_Miscellaneousattributes_table" class="collection-table">
<a name="attributes" class="anchor"></a>
<h3> ${count} Attributes </h3>
<div class="clear"></div>
<div id="coll_im_aspect_Miscellaneousattributes">
<div id="coll_im_aspect_Miscellaneousattributes_inner" style="overflow-x:auto;">

<c:set var="wikiurl"
value="http://wiki.modencode.org/project/index.php/Special:Filepath/" />

<table >
<tr>
<th>Name</th>
<th>Value</th>
<th>Type</th>
</tr>
<c:forEach items="${object.dataAttributes}" var="att" varStatus="a_status">
<c:set var="pRowClass">
<c:choose>
<c:when test="${p_status.count % 2 == 1}">
odd-alt
</c:when>
<c:otherwise>
even-alt
</c:otherwise>
</c:choose>
</c:set>

<tr class="<c:out value="${pRowClass}"/>">
<td>${att.name}</td>
<td>
<c:choose>
<c:when test="${fn:startsWith(att.name, 'QC image')}" >

<c:set var="image"
value="${fn:substringAfter(att.value,'http://wiki.modencode.org/project/index.php?title=Image:')}" />

<a href="${wikiurl}${image}" target="_blank" class="value extlink">
<img src="${wikiurl}${image}" height=150 width=150 alt="${image}" />
</a>

</c:when>
<c:otherwise>
${att.value}
</c:otherwise>
</c:choose>
</td>
<td>
${att.type}
</a>
</td>
</tr>
</c:forEach>
</table>

</div>

</div>
<div class="clear"></div>
</div>

<!-- /dataAttribute.jsp -->