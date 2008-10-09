<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<!-- bagTagSelect.jsp -->
<%-- Tile for displaying list tags and filtering lists according to the selected tag --%>

<tiles:importAttribute name="selectId" ignore="false"/>
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
	<tiles:put name="onChangeFunction" value="filterByTag" />
	<tiles:put name="selectId" value="${selectId}" />
</tiles:insert>                

<!-- /bagTagSelect.jsp -->