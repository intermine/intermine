<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<tiles:importAttribute scope="request"/>

<!-- objectDetails.jsp -->
<font class="resultsCellTitle">
  <c:forEach var="cld" items="${leafClds}">
    <c:out value="${cld.unqualifiedName}"/>
  </c:forEach>
</font>
<br/>
<br/>
<c:forEach var="cld" items="${leafClds}">
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
</c:forEach>
<!-- /objectDetails.jsp -->
