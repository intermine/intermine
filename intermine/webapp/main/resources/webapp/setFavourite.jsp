<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/shared/taglibs.jsp" %>

<!--setFavourite.jsp-->

<c:if test="${PROFILE.loggedIn}">

  <tiles:importAttribute name="name" ignore="false"/>
  <tiles:importAttribute name="type" ignore="false"/>
  <c:set var="name" value="${fn:replace(name,'\\'','#039;')}"/>

  <c:choose>
    <c:when test="${isFavourite == 'true'}">
  <img id="favourite_<c:out value="${name}" escapeXml="true"/>" src="images/star_active.gif" style="cursor:pointer;" onclick="setFavourite('<c:out value="${name}" escapeXml="true"/>','<c:out value="${type}" escapeXml="true"/>',this)" title="Click here to remove this item from your Favourites"/>
    </c:when>
    <c:otherwise>
  <img id="favourite_<c:out value="${name}" escapeXml="true"/>" src="images/star_unactive.gif" style="cursor:pointer;" onclick="setFavourite('<c:out value="${name}" escapeXml="true"/>','<c:out value="${type}" escapeXml="true"/>',this)"  title="Click here to Set as favourite"/>
    </c:otherwise>
  </c:choose>

</c:if>

<!--/setFavourite.jsp-->
