<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>

<!-- debtField.jsp -->
<div>
  <nobr>
    <c:out value="${fieldDescriptor.name}"/>:
    <span class="debt">
      <c:out value="${object[fieldDescriptor.name]}" default="null"/>
    </span>
  </nobr>
</div>
<!-- /debtField.jsp -->
