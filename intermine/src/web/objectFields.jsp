<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- objectFields.jsp -->
<html:xhtml/>
<c:forEach items="${cld.allFieldDescriptors}" var="fieldDescriptor">
  <c:set var="fieldName" value="${fieldDescriptor.name}"/>
  <c:if test="${viewType eq 'detail' || primaryKeyFields[fieldName] != null}">
    <div>
      <c:set var="fieldDescriptor" value="${fieldDescriptor}" scope="request"/>
      <c:set var="object" value="${object}" scope="request"/>
      <c:set var="cld" value="${cld}" scope="request"/>
      <tiles:insert name="/oneField.jsp"/>
    </div>
  </c:if>
</c:forEach>
<!-- /objectFields.jsp -->
