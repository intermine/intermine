<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-bean-el.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>

<!-- oneField.jsp -->
<div class="oneField">
  <c:choose>
    
    <c:when test="${fieldDescriptor.attribute}">
      <c:if test="${fieldDescriptor.name != 'id'}">
        <div>
          <c:out value="${fieldDescriptor.name}: ${object[fieldDescriptor.name]}"/>
        </div>
      </c:if>
    </c:when>
    
    <c:when test="${fieldDescriptor.reference}">
      <c:if test="${object[fieldDescriptor.name] != null}">
        <div>
          <nobr>
            <c:out value="${fieldDescriptor.name}"/>:
            <html:link action="/objectDetails?id=${object.id}&field=${fieldDescriptor.name}">
              <c:out value="${fieldDescriptor.referencedClassDescriptor.unqualifiedName}"/>
            </html:link>
          </nobr>
        </div>
      </c:if>
    </c:when>
    
    <c:when test="${fieldDescriptor.collection}">
      <bean:size collection="${object[fieldDescriptor.name]}" id="listSize"/>
      <c:if test="${listSize > 0}">
        <div>
          <nobr>
            <c:out value="${fieldDescriptor.name}"/>:
            <html:link action="/viewCollection?id=${object.id}&field=${fieldDescriptor.name}">
              <c:out value="${fieldDescriptor.referencedClassDescriptor.unqualifiedName}[${listSize}]"/>
            </html:link>
          </nobr>
        </div>
      </c:if>
    </c:when>
    
  </c:choose>
</div>
<!-- /oneField.jsp -->
