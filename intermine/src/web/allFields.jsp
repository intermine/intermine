<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>

<tiles:importAttribute/>

<!-- allFields.jsp -->
<c:forEach items="${cld.allFieldDescriptors}" var="field" varStatus="status">
  <c:if test="${field.name != 'id'}">
    <%-- ignore collections for the moment --%>
    <c:if test="${!field.collection}">
      <font class="resultsCellName">
        <c:out value="${prefix}${field.name}"/>
      </font>
      =
      <font class="resultsCellValue">
        <c:choose>
          <c:when test="${field.attribute}">
            <c:out value="${object[field.name]}"/>
          </c:when>
          <c:when test="${field.reference}">
            <html:link action="/details?id=${object.id}&field=${field.name}">
              <c:out value="${field.referencedClassDescriptor.unqualifiedName}"/>
            </html:link>
          </c:when>
        </c:choose>
      </font>
      <br/>
    </c:if>
  </c:if>
</c:forEach>

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
<!-- /allFields.jsp -->
