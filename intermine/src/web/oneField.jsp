<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-bean-el.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>

<!-- oneField.jsp -->
<div class="oneField">
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
</div>
<!-- /oneField.jsp -->
