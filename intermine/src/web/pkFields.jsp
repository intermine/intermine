<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%--/**
     * Render the primary key fields of an object
     */
--%>

<tiles:importAttribute/>

<font class="resultsCellTitle">
  <c:out value="${cld.unqualifiedName}"/>
</font>
<br/>
<c:forEach var="field" items="${cld.pkFieldDescriptors}" varStatus="status">
  <font class="resultsCellName">
    <c:out value="${field.name}"/>
  </font>
  =
  <font class="resultsCellValue">
    <c:out value="${object[field.name]}"/>
  </font>
  <br/>
</c:forEach>
