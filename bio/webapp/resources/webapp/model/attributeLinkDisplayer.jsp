<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!-- attributeLinkDisplayer.jsp -->
<fmt:setBundle basename="model"/>

<c:set var="genus" value="${object.organism.genus}"/>
<c:set var="species" value="${object.organism.species}"/>
<c:choose>
  <c:when test="${genus == null || species == null}">
    <c:set var="linkKey" value="${attributeLinkClassName}"/>
  </c:when>
  <c:otherwise>
    <c:set var="linkKey" value="${attributeLinkClassName}.${genus}.${species}"/>
  </c:otherwise>
</c:choose>
<c:set var="attrConfigMap" value="${attributeLinkConfiguration[linkKey]}"/>

<div style="margin-left: 20px">
  <c:forEach var="attMapEntry" items="${attrConfigMap}">
    <c:set var="attName" value="${attMapEntry.key}"/>
    <c:set var="href" value="${attMapEntry.value.url}"/>
    <c:set var="imageName" value="${attMapEntry.value.imageName}"/>
    <c:set var="text" value="${attMapEntry.value.text}"/>

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

  </c:forEach>
</div>
<!-- /attributeLinkDisplayer.jsp -->
