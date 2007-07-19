<%@ include file="/shared/taglibs.jsp" %>

<!--starTemplate.jsp-->

<c:if test="${! empty PROFILE.username}">

	<tiles:importAttribute name="templateName" ignore="false"/>
	
	<c:set var="templateName" value="${fn:replace(templateName,'\\'','#039;')}" />
	
	<c:choose>
		<c:when test="${isFavourite == 'true'}">
	<img id="favourite_${templateName}" src="images/star_active.gif" title="Favourite"/>
		</c:when>
		<c:otherwise>
	<img id="favourite_${templateName}" style="cursor:pointer;" onclick="setFavouriteTemplate('${templateName}',this)" src="images/star_unactive.gif" title="Set as favourite"/>
		</c:otherwise>
	</c:choose>
</c:if>

<!--/starTemplate.jsp-->