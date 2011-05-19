<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="mm"%>

<!-- tracksDisplayer tile starts -->
<link rel="stylesheet" type="text/css" href="model/css/experiment.css"/>

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
     <mm:getTarball dccId="${object.dCCid}"/>
</table>

<!-- tracksDisplayer tile ends -->