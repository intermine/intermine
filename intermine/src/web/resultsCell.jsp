<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%--/**
     * Render a results cell
     */
--%>

<tiles:importAttribute scope="request"/>

<c:choose>
  <c:when test="${cld != null}">
  <%-- Go through all the items in the WebConfig for this object --%>
  <table><tr>
    <c:if test="${empty webconfig.types[cld.name].shortDisplayers}">
      <td><tiles:insert name="/pkFields.jsp" /></td>
    </c:if>
    <c:forEach items="${webconfig.types[cld.name].shortDisplayers}" var="displayer">
      <td><tiles:insert beanName="displayer" beanProperty="src"/></td>
    </c:forEach>
  </tr></table>

  </c:when>
  <c:otherwise>
    <font class="resultsCellValue">
      <c:out value="${object}"/>
    </font>
  </c:otherwise>
</c:choose>

