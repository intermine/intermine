<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

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

