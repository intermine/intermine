<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%--/**
     * Render all the fields of an object
     */
--%>

<tiles:importAttribute/>

<font class="resultsCellTitle">
  <c:out value="${cld.unqualifiedName}"/>
</font>
<br/>
<c:forEach var="field" items="${cld.allFieldDescriptors}" varStatus="status">
  <c:if test="${object[field.name] != null}">
    <font class="resultsCellName">
      <c:out value="${field.name}"/>
    </font>
    =
    <font class="resultsCellValue">
      <c:out value="${object[field.name]}"/>
    </font>
    <br/>
  </c:if>
</c:forEach>
