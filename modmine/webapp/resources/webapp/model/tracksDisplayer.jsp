<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="mm"%>

<link rel="stylesheet" type="text/css" href="model/css/experiment.css"/>    

<%-- GBROWSE --%>
          <table cellpadding="0" cellspacing="0" border="0" class="internal" >
			<tr>
			<td valign="top">GBrowse tracks: </td>
			<td valign="top">
		        <c:forEach var="track" items="${subTracks}" varStatus="track_status">
					<mm:singleTrack track="${track}"/>
			        <br>
	            </c:forEach>
            </td>
            <td valign="top">
                <mm:allTracks tracks="${subTracks}" dccId="${sub.dCCid}"/>
            </td>
            <td valign="top" align="right">Data files: </td>
            <td valign="top">
            
              <%-- FILES --%>          
	          <span class="filelink">
	            <mm:dataFiles files="${files}" dccId="${sub.dCCid}"/>
	          </span>
            </td>
            </tr>
            
          </table>
          
          
            
           
          

