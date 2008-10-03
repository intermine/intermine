<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<!-- bagTagSelect.jsp -->
<%-- Tile for displaying list tags and filtering lists according to the selected tag -->

<script type="text/javascript">
	function filterByTag(tag) {
		var callBack = function(filteredList) {
			setSelectElement('bagSelect', '', filteredList);
		}
		AjaxServices.filterByTag('bag', tag, callBack);
	}
</script>

<tiles:insert name="tagSelect.tile">
	<tiles:put name="type" value="bag" />
	<tiles:put name="onChange" value="filterByTag" />
</tiles:insert>                

<!-- /bagTagSelect.jsp -->