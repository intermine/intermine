<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- geneLong.jsp -->
<fmt:setBundle basename="model"/>

<html:link action="/objectDetails?id=${object.organism.id}">
  <c:out value="${object.organism.name}"/>
</html:link>
<fmt:message key="gene.gene"/>
<c:if test="${!empty object.name}">
  <c:out value="${object.name}"/>
</c:if>
<br/>

<c:if test="${!empty object.synonyms}">
  <br/>
  <fmt:message key="gene.synonyms"/>:<br/>
  <ul>
    <c:forEach items="${object.synonyms}" var="thisSynonym">
      <c:set var="sourceTitle" value="${thisSynonym.source.title}"/>
      <c:set var="linkProperty" value="${sourceTitle}.${object.organism.genus}.${object.organism.species}.url.prefix"/>
      <li>
        <html:img src="model/${sourceTitle}_logo_small.png"/>
        <c:choose>
          <c:when test="${empty WEB_PROPERTIES[linkProperty]}">
            <c:out value="${thisSynonym.value}"/>
          </c:when>
          <c:otherwise>
            <html:link href="${WEB_PROPERTIES[linkProperty]}${thisSynonym.value}"
                       title="${sourceTitle}: ${thisSynonym.value}"
                       target="view_window">
              <c:out value="${thisSynonym.value}"/>
            </html:link>
          </c:otherwise>
        </c:choose>
      </li>
    </c:forEach>
  </ul>
</c:if>
<!-- /geneLong.jsp -->
