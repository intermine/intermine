<%@ tag body-content="scriptless"  %>
<%@ attribute name="title" required="false" %>
<%@ attribute name="titleKey" required="false" %>
<%@ attribute name="titleLink" required="false" %>
<%@ attribute name="stylename" required="true" %>
<%@ attribute name="minWidth" required="false" %>
<%@ attribute name="fixedWidth" required="false" %>
<%@ attribute name="floatValue" required="false" %>
<%@ attribute name="htmlId" required="false" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>


<c:if test="${!empty titleKey}">
  <fmt:message key="${titleKey}" var="title"/>
</c:if>
<c:set var="extraStyle">
  <c:if test="${! empty minWidth}">min-width:${minWidth};</c:if>
  <c:if test="${! empty fixedWidth}">width:${fixedWidth};</c:if>
  <c:if test="${! empty floatValue}">float:${floatValue};</c:if>
</c:set>
 <div class="${stylename}" style="${extraStyle}" >
  <dl>
    <dt><h1 id="${htmlId}">
      <c:choose>
        <c:when test="${!empty titleLink}">
          <html:link action="${titleLink}">${title}</html:link>
        </c:when>
        <c:otherwise>
          <c:out value="${title}"/>
        </c:otherwise>
      </c:choose>
    </h1></dt>
    <dd><jsp:doBody/></dd>
  </dl>
 </div>
