<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- addressField.jsp -->
<div class="address">
  <nobr>
    <c:out value="${fieldDescriptor.name}"/>:
    <span>
      <c:out value="${object[fieldDescriptor.name]}" default="null"/>
    </span>
  </nobr>
</div>
<!-- /addressField.jsp -->
