<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%--/**
     * Render details of an object
     */
--%>

<tiles:importAttribute scope="request"/>

<c:choose>
  <c:when test="${cld != null}">
  <%-- Go through all the items in the WebConfig for this object --%>
    <c:if test="${empty webconfig.types[cld.name].longDisplayers}">
      <tiles:insert name="/allFields.jsp" />
    </c:if>
    <c:forEach items="${webconfig.types[cld.name].longDisplayers}" var="displayer">
      <tiles:insert beanName="displayer" beanProperty="src"/>
    </c:forEach>
  </c:when>
  <c:otherwise>
    <font class="resultsCellValue">
      <c:out value="${object}"/>
    </font>
  </c:otherwise>
</c:choose>

