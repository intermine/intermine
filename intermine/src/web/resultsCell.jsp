<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<%--/**
     * Render a results cell
     * The following parameters must be set:
     * fields: a List of fields to render
     * icons: a List of icons to display
     */
--%>

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
