<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- tagSelect.jsp -->

<%--Tile usage: 
    type parameter - is type of objects for which you want to display tags like 'bag', 'template' 
    onChangeFunction parameter - is name of function that you want to be called when the select is changed, 
        you must define this function with exactly one parameter - values of new select
    disabled - if you want select to be disabled 
    call reloadTagSelect(selectId, type) function if you want select to be reloaded --%>

<tiles:importAttribute name="type" ignore="false" />
<tiles:importAttribute name="selectId" ignore="false" />
<tiles:importAttribute name="onChangeFunction" ignore="true" />
<tiles:importAttribute name="disabled" ignore="true" />

<script type="text/javascript" src="js/imdwr.js" ></script>
<script type="text/javascript" src="js/tagSelect.js" ></script>

<c:choose>
	<c:when test="${!empty onChangeFunction}">
		<select id="${selectId}" onchange="javacript:callOnChangeFunction('${selectId}', '${onChangeFunction}')"></select>
	</c:when>
	<c:otherwise>
		<select id="${selectId}"></select>
	</c:otherwise>
</c:choose>

<script type="text/javascript">
	displayTagSelect("${selectId}", "${type}");
</script>

<c:if test="${!empty disabled}">
	<script type="text/javascript">
		document.getElementById("${selectId}").disabled = true;
	</script>	
</c:if>

<!-- /tagSelect.jsp -->