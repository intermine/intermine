<%@ tag body-content="empty"%>
<%@ attribute name="tracks" required="true" type="java.util.List" %>
<%@ attribute name="dccId" required="true" type="java.lang.String" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<c:set var="urlabels" value=""/>
<c:set var="organism" value=""/>
<c:forEach var="track" items="${tracks}" varStatus="track_status">
     <c:set var="organism" value="${track.organism}"/>
     <c:choose>
     <c:when test="${track_status.first}">
          <c:set var="urlabels" value="${track.track}/${track.subTrack}" /> 
     </c:when>
     <c:otherwise>
     <%-- checking if coming from different tracks --%>
     <c:choose>
     <c:when test="${fn:contains(urlabels,track.track)}">
          <c:set var="urlabels" value="${urlabels}-${track.subTrack}" /> 
     </c:when>
     <c:otherwise>
          <c:set var="urlabels" value="${urlabels}-${track.track}/${track.subTrack}" /> 
     </c:otherwise>
     </c:choose>
     </c:otherwise>
     </c:choose>
</c:forEach>

<c:if test="${!empty tracks}">
<html:link
  href="${WEB_PROPERTIES['gbrowse.prefix']}/${organism}/?label=${urlabels}" target="_blank" title="Titolo">
  <html:img src="model/images/${organism}_gb.png" title="View all tracks for submission ${dccId} in GBrowse"/>
</html:link>
</c:if>