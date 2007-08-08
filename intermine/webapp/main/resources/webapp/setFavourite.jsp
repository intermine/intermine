<%@ include file="/shared/taglibs.jsp" %>

<!--setFavourite.jsp-->

<c:if test="${! empty PROFILE.username}">

	<tiles:importAttribute name="name" ignore="false"/>
	<tiles:importAttribute name="type" ignore="false"/>
	
	<c:set var="name" value="${fn:replace(name,'\\'','#039;')}" />
	
	<c:choose>
		<c:when test="${isFavourite == 'true'}">
	<img id="favourite_${name}" src="images/star_active.png" style="cursor:pointer;" onclick="setFavourite('${name}','${type}',this)" title="Favourite"/>
		</c:when>
		<c:otherwise>
	<img id="favourite_${name}" src="images/star_unactive.png" style="cursor:pointer;" onclick="setFavourite('${name}','${type}',this)"  title="Set as favourite"/>
		</c:otherwise>
	</c:choose>
</c:if>

<!--/setFavourite.jsp-->