<%@ include file="/shared/taglibs.jsp" %>

<!--starTemplate.jsp-->

<c:if test="${! empty PROFILE.username}">
	<script type='text/javascript' src='dwr/interface/AjaxServices.js'></script>
	<script type='text/javascript' src='dwr/engine.js'></script>
	<script type='text/javascript' src='dwr/util.js'></script>
	<script language="javascript">
		function setFavouriteTemplate(templateName, image){
			AjaxServices.setFavouriteTemplate(templateName);
			image.src='images/star_active.gif';
			image.onclick='';
			image.style.cursor='';
			image.title='This template is a favourite';
		}
	</script>
	
	<tiles:importAttribute name="templateName" ignore="false"/>
	
	<c:set var="templateName" value="${fn:replace(templateName,'\\'','#039;')}" />
	
	<c:choose>
		<c:when test="${isFavourite == 'true'}">
	&nbsp;<img class="arrow" id="favourite_${templateName}" src="images/star_active.gif" title="Favourite"/>
		</c:when>
		<c:otherwise>
	&nbsp;<img class="arrow" id="favourite_${templateName}" style="cursor:pointer;" onclick="setFavouriteTemplate('${templateName}',this)" src="images/star_unactive.gif" title="Set as favourite"/>
		</c:otherwise>
	</c:choose>
</c:if>

<!--/starTemplate.jsp-->