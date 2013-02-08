<%@ tag body-content="empty"%>
<%@ attribute name="tracks" required="true" type="java.util.List" %>
<%@ attribute name="dccId" required="true" type="java.lang.String" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<c:set var="tracks_sep" value="%2F"/>
<c:set var="tracksubtrack_sep" value="%1E"/>
<c:set var="subtracks_sep" value="%20"/>
<c:set var="urlabels" value=""/>
<c:set var="organism" value=""/>
<c:forEach var="track" items="${tracks}" varStatus="track_status">
     <c:set var="organism" value="${track.organism}"/>
     <c:choose>
     <c:when test="${track_status.first}">
          <c:set var="urlabels" value="${track.track}${tracksubtrack_sep}${track.subTrack}" /> 
     </c:when>
     <c:otherwise>
     <%-- checking if coming from different tracks --%>
     <c:choose>
     <c:when test="${fn:contains(urlabels,track.track)}">
          <c:set var="urlabels" value="${urlabels}${subtracks_sep}${track.subTrack}" /> 
     </c:when>
     <c:otherwise>
          <c:set var="urlabels" 
          value="${urlabels}${tracks_sep}${track.track}${tracksubtrack_sep}${track.subTrack}" /> 
     </c:otherwise>
     </c:choose>
     </c:otherwise>
     </c:choose>
</c:forEach>

<c:if test="${!empty tracks}">

<c:set var="pic" value="fly" />
<c:if test="${fn:startsWith(organism,'c') || organism=='worm'}">
<c:set var="pic" value="worm" />
</c:if>

<html:link styleId="sub-all-tracks"
  href="${WEB_PROPERTIES['gbrowse.prefix']}/${organism}/?l=${urlabels}" target="_blank" title="${dccId}">
  <html:img src="model/images/${pic}_gb.png" title="View all tracks for submission ${dccId} in GBrowse"/>
</html:link>
</c:if>