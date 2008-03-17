<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!-- attributeLinkDisplayer.jsp -->
<fmt:setBundle basename="model"/>

<table class="lookupReport" cellspacing="5" cellpadding="0">
  <c:forEach var="confMapEntry" items="${attributeLinkConfiguration}">
    <c:set var="href" value="${confMapEntry.value.url}"/>
    <c:set var="imageName" value="${confMapEntry.value.imageName}"/>
    <c:set var="text" value="${confMapEntry.value.text}"/>

    <c:if test="${!empty confMapEntry.value.valid && !empty confMapEntry.value.attributeValue}">
    <tr>
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
    </tr>
    </c:if>
  </c:forEach>
</table>
<!-- /attributeLinkDisplayer.jsp -->
