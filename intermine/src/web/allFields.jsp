<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-bean-el.tld" prefix="bean" %>

<tiles:importAttribute/>

<!-- allFields.jsp -->
<c:forEach items="${cld.allFieldDescriptors}" var="field" varStatus="status">
  <c:choose>
    
    <c:when test="${field.attribute}">
      <c:if test="${field.name != 'id'}">
        <c:out value="${field.name}: ${object[field.name]}"/>
        <br/>
      </c:if>
    </c:when>
    
    <c:when test="${field.reference}">
      <c:if test="${object[field.name] != null}">
        <c:out value="${field.name}:"/>
        <html:link action="/details?id=${object.id}&field=${field.name}">
          <c:out value="${field.referencedClassDescriptor.unqualifiedName}"/>
        </html:link>
        <br/>
      </c:if>
    </c:when>
    
    <c:when test="${field.collection}">
      <bean:size collection="${object[field.name]}" id="listSize"/>
      <c:if test="${listSize > 0}">
        <c:out value="${field.name}: ${field.referencedClassDescriptor.unqualifiedName}[${listSize}]"/>
      <br/>
      </c:if>
    </c:when>
    
  </c:choose>
</c:forEach>
<!-- /allFields.jsp -->
