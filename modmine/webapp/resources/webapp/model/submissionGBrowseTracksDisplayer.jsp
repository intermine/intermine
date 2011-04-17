<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="mm"%>

<!-- submissionGBrowseTracksDisplayer.jsp -->

<link rel="stylesheet" type="text/css" href="model/css/experiment.css"/>

<script type="text/javascript">

    jQuery(document).ready(function(){
        var gb_link = jQuery('#sub-all-tracks').attr('href');
        if (gb_link != "") {
            jQuery('#sub-gbrowse-table').append('<div><a href="' + jQuery('#sub-all-tracks').attr('href') + '" TARGET=_BLANK><img title="GBrowse" style="border: 1px solid black;" src="' + gb_link.replace(/gbrowse/, "gbrowse_img") + '"></a></div>');
        }
    });


</script>

<table id="sub-gbrowse-table" cellpadding="0" cellspacing="0" border="0" class="internal" >
    <%-- GBROWSE --%>
    <c:if test="${!empty subTracks}">
        <tr valign="top">GBrowse tracks: (to be reloacted at the right side bar?)</tr>
        <tr>
            <td valign="middle">
                <c:forEach var="track" items="${subTracks}" varStatus="track_status">
                    <mm:singleTrack track="${track}"/>
                    <br>
                </c:forEach>
            </td>
            <td valign="middle">
                <mm:allTracks tracks="${subTracks}" dccId="${object.dCCid}"/>
            </td>
        </tr>
    </c:if>
      <%-- FILES --%>
    <c:if test="${!empty files}">
         <tr valign="top" align="right">Data files: </tr>
         <tr valign="top">
             <span class="filelink">
                <mm:dataFiles files="${files}" dccId="${object.dCCid}"/>
             </span>
         </tr>
    </c:if>
</table>

<!-- /submissionGBrowseTracksDisplayer.jsp -->