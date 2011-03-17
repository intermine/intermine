<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="mm"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>

<!-- tracksDisplayer tile starts -->
<link rel="stylesheet" type="text/css" href="model/css/experiment.css"/>    

<%-- set a DEFAULT ftp.url property --%>
<c:choose>
<c:when test="${fn:length(WEB_PROPERTIES['ftp.url']) gt 5 }" >
<c:set var="ftpURL" value="${WEB_PROPERTIES['ftp.url']}" />
</c:when>
<c:otherwise>
<c:set var="ftpURL" value="http://submit.modencode.org/submit/public" />
</c:otherwise>
</c:choose>

<%-- GBROWSE --%>
    <table cellpadding="0" cellspacing="0" border="0" class="internal" >
			<tr>
			<c:if test="${!empty subTracks}">
			<td valign="top">GBrowse tracks: </td>
			<td valign="top">
	        <c:forEach var="track" items="${subTracks}" varStatus="track_status">
   					<mm:singleTrack track="${track}"/>
		        <br>
	        </c:forEach>
      </td>
      <td valign="top">
            <mm:allTracks tracks="${subTracks}" dccId="${object.dCCid}"/>
      </td>
      </c:if>
<%-- FILES --%> 
      <c:if test="${!empty files}">
         <td valign="top" align="right">Data files: </td>
         <td valign="top">
         <span class="filelink">
            <mm:dataFiles files="${files}" dccId="${object.dCCid}"/>
         </span>
         </td>
      </c:if>
      </tr>
<%-- TARBALL --%>      
 <tr><td colspan=2>
     <a href="${ftpURL}/download_tarball/${fn:substringAfter(object.dCCid, 'modENCODE_')}.tgz?structured=true"
          title="Download all data files (tarball)" class="value extlink"> 
          <c:out value="Download all data files" /> </a>
      
   </table>
          
<!-- tracksDisplayer tile ends -->        
            
           
          

