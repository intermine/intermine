<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- currentTags.jsp -->

<%-- currentTags variables are set by inlineTagEditorController, this could be implemented without
 it but it would cause 100 AJAX calls if there are 100 tags. So when the page is generated tags
 are obtained usinginlineTagEditorController, for refresh is used AjaxServices.getObjectTags  --%>
<script type="text/javascript">
  <c:forEach items="${currentTags}" var="item" varStatus="status">
     addTagSpan('${editorId}', '${type}', '${item}');
  </c:forEach>
</script>

<!-- /currentTags.jsp -->

