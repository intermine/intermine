<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>

<tiles:importAttribute scope="request"/>

<!-- details.jsp -->
<c:choose>
  <c:when test="${object != null}">
    <c:forEach var="cld" items="${leafClds}">
      <c:out value="${cld.unqualifiedName}"/>
      <br/>
      <br/>
      <c:choose>
        <c:when test="${!empty webconfig.types[cld.name].longDisplayers}">
          <c:forEach items="${webconfig.types[cld.name].longDisplayers}" var="displayer">
            <c:set var="cld" value="${cld}" scope="request"/>
            <tiles:insert beanName="displayer" beanProperty="src"/>
          </c:forEach>
        </c:when>
        <c:otherwise>
          <c:set var="cld" value="${cld}" scope="request"/>
          <tiles:insert name="/allFields.jsp"/>
        </c:otherwise>
      </c:choose>
      <br/>
    </c:forEach>
  </c:when>
  <c:otherwise>
    null
  </c:otherwise>
</c:choose>
<br/>
<html:link action="/results">
  <fmt:message key="results.return"/>
</html:link>
<!-- /details.jsp -->