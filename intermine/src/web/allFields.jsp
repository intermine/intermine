<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%--/**
     * Render the primary key fields of an object
     */
--%>

<tiles:importAttribute/>

<c:forEach var="field" items="${cld.allFieldDescriptors}" varStatus="status">
  <c:choose>
    <c:when test="${field.attribute}">
      <font class="resultsCellName">
        <c:out value="${prefix}${field.name}"/>
      </font>
      =
      <font class="resultsCellValue">
        <c:out value="${object[field.name]}"/>
      </font>
    </c:when>
    <c:when test="${field.reference}">
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
    </c:when>
    <c:otherwise>

    </c:otherwise>
  </c:choose>
  <br/>
</c:forEach>
