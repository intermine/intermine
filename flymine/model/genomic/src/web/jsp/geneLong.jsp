<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>

<!-- geneLong.jsp -->
<fmt:setBundle basename="model"/>

<br/>
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
      <html:link href="${WEB_PROPERTIES[linkProperty]}${thisSynonym.synonym}"
                 title="${sourceTitle}: ${thisSynonym.synonym}"
                 target="view_window">
        <c:out value="${thisSynonym.synonym}"/>
      </html:link>
    </li>
  </c:forEach>
  </ul>
</c:if>
<!-- /geneLong.jsp -->
