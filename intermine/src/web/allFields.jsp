<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<tiles:importAttribute/>

<!-- allFields.jsp -->
<c:forEach items="${cld.allFieldDescriptors}" var="field" varStatus="status">
  <c:choose>
    <c:when test="${field.attribute}">
      <font class="resultsCellName">
        <c:out value="${prefix}${field.name}"/>
      </font>
      =
      <font class="resultsCellValue">
        <c:out value="${object[field.name]}"/>
      </font>
      <br/>
    </c:when>
    <%--c:when test="${field.reference}">
    <c:set var="thisprefix" value="${prefix}" scope="page"/>
    <c:set var="thiscld" value="${cld}" scope="page"/>
    <c:set var="thisobject" value="${object}" scope="page"/>
    <c:set var="prefix" value="${prefix}${field.name}." scope="request"/>
    <c:set var="cld" value="${field.referencedClassDescriptor}" scope="request"/>
    <c:set var="object" value="${object[field.name]}" scope="request"/>
    <tiles:insert name="/allFields.jsp" />
    <c:set var="cld" value="${thiscld}" scope="request"/>
    <c:set var="prefix" value="${thisprefix}" scope="request"/>
    <c:set var="object" value="${thisobject}" scope="request"/>
    </c:when--%>
  </c:choose>
</c:forEach>
<!-- /allFields.jsp -->
