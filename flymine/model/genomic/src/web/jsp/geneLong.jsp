<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- geneLong.jsp -->
<fmt:setBundle basename="model"/>

<c:if test="${!empty object.synonyms}">
  <fmt:message key="gene.synonyms"/>:
  <ul>
    <c:forEach items="${object.synonyms}" var="thisSynonym">
      <c:set var="sourceTitle" value="${thisSynonym.source.title}"/>
      <c:set var="linkProperty" value="${sourceTitle}.${object.organism.genus}.${object.organism.species}.url.prefix"/>
      <c:if test="${!empty WEB_PROPERTIES[linkProperty]}">
        <li>
          <html:img src="model/${sourceTitle}_logo_small.png"/>
          <html:link href="${WEB_PROPERTIES[linkProperty]}${thisSynonym.value}"
                     title="${sourceTitle}: ${thisSynonym.value}"
                     target="view_window">
            <c:out value="${thisSynonym.value}"/>
          </html:link>
        </li>
      </c:if>
    </c:forEach>
  </ul>
</c:if>
<!-- /geneLong.jsp -->
