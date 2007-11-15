<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!-- attributeLinkDisplayer.jsp -->
<fmt:setBundle basename="model"/>

<div style="margin-left: 20px">
  <c:forEach var="confMapEntry" items="${attributeLinkConfiguration}">
    <c:set var="href" value="${confMapEntry.value.url}"/>
    <c:set var="imageName" value="${confMapEntry.value.imageName}"/>
    <c:set var="text" value="${confMapEntry.value.text}"/>

    <c:if test="${!empty confMapEntry.value.valid && !empty confMapEntry.value.attributeValue}">
      <div>
        <c:if test="${!empty href}">
          <a href="${href}">
        </c:if>
        <c:if test="${!empty imageName}">
          <html:img src="model/images/${imageName}"/>
        </c:if>
        <c:if test="${!empty text}">
          ${text}
        </c:if>
        <c:if test="${!empty href}">
        </a>
        </c:if>
      </div>
    </c:if>
  </c:forEach>
</div>
<!-- /attributeLinkDisplayer.jsp -->
