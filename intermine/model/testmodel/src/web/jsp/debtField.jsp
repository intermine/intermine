<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- debtField.jsp -->
<span class="debt">
  <c:out value="${object[fieldDescriptor.name]}" default="null"/>
</span>
<!-- /debtField.jsp -->
