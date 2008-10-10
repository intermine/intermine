<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- currentTags.jsp -->

<script type="text/javascript">
function deleteTag(tagId, uid, type) {
	var callBack = function(success) {
		if (success) {
			refreshTags(uid, type);
		} else {
			window.alert('Deleting tag failed.');
		}
	}
	AjaxServices.deleteTag(tagId, callBack);
}
</script>

<c:forEach items="${currentTags}" var="item" varStatus="status">
  <span class="tag">${item.tagName} <a href="#" class="deleteTagLink" onclick="deleteTag('${item.id}', '${uid}', '${type}');return false;">[x]</a></span>
</c:forEach>

<!-- /currentTags.jsp -->

