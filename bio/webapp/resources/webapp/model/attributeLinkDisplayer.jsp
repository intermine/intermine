<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- attributeLinkDisplayer.jsp -->
<fmt:setBundle basename="model"/>
<table class="lookupReport" cellspacing="5" cellpadding="0">
  <c:forEach var="confMapEntry" items="${attributeLinkConfiguration}">
    <c:set var="href" value="${confMapEntry.value.url}"/>
    <c:set var="imageName" value="${confMapEntry.value.imageName}"/>
    <c:set var="text" value="${confMapEntry.value.text}"/>
    <c:set var="parameters" value="${confMapEntry.value.parameters}"/>
    <c:set var="usePost" value="${confMapEntry.value.usePost}"/>
    <c:set var="linkId" value="${confMapEntry.value.linkId}"/>

    <c:if test="${!empty confMapEntry.value.valid && !empty confMapEntry.value.attributeValue}">
    <tr>
        <c:choose>

	        <%-- GET form --%>
	        <c:when test="${empty usePost}">
	            <td align="right">
		            <c:if test="${!empty imageName}">
		              <a href="${href}" class="ext_link" target="_new"><html:img src="model/images/${imageName}" title="${text}"/></a>
		            </c:if>
	            </td>
	            <td>
		            <c:if test="${!empty text}">
		              <a href="${href}" class="ext_link" target="_new">${text}&nbsp;<img src="images/ext_link.png" title="${text}"/></a>
		            </c:if>
	            </td>
	        </c:when>

	        <%-- POST form --%>
	        <c:otherwise>
                <td align="right">
	                <c:if test="${!empty imageName}">
	                  <a href="javascript:document.getElementById('${linkId}Form').submit();" class="ext_link" >
	                           <html:img src="model/images/${imageName}" title="${text}"/>
	                  </a>
	                </c:if>
	            </td>
                <td>
	                <c:if test="${!empty text}">
	                  <a href="javascript:document.getElementById('${linkId}Form').submit();" class="ext_link" >
	                        ${text}&nbsp;<img src="images/ext_link.png" title="${text}"/>
	                  </a>
	                </c:if>
                </td>

                <form action="${href}" method="post" id="${linkId}Form" target="_blank">
                     <c:forEach var="par" items="${parameters}">
                         <input type="hidden" value="${par.value}" name="${par.key}" />
                     </c:forEach>
                </form>
	        </c:otherwise>
        </c:choose>
    </tr>
    </c:if>
  </c:forEach>
</table>
<!-- /attributeLinkDisplayer.jsp -->
