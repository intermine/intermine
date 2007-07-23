<%@ include file="/shared/taglibs.jsp" %>

<!--starTemplate.jsp-->

<c:if test="${! empty PROFILE.username}">

	<tiles:importAttribute name="name" ignore="false"/>
	<tiles:importAttribute name="type" ignore="false"/>
	
	<c:set var="name" value="${fn:replace(name,'\\'','#039;')}" />
	
	<c:choose>
		<c:when test="${isFavourite == 'true'}">
	<img id="favourite_${name}" src="images/star_active.gif" title="Favourite"/>
		</c:when>
		<c:otherwise>
	<img id="favourite_${name}" style="cursor:pointer;" onclick="setFavourite('${name}','${type}',this)" src="images/star_unactive.gif" title="Set as favourite"/>
		</c:otherwise>
	</c:choose>
</c:if>

<!--/starTemplate.jsp-->