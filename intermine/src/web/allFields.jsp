<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<tiles:importAttribute/>

<!-- allFields.jsp -->
<c:forEach items="${cld.allFieldDescriptors}" var="thisFieldDescriptor" varStatus="status">
  <c:set var="fieldDescriptor" value="${thisFieldDescriptor}" scope="request"/>
  <tiles:insert name="/oneField.jsp"/>
</c:forEach>
<!-- /allFields.jsp -->
