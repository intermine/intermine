<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="mm"%>

<!-- submissionExternalLinksDisplayer.jsp -->

<link rel="stylesheet" type="text/css" href="model/css/experiment.css"/>

<style>

/* ref - http://matthewjamestaylor.com/blog/perfect-3-column-blog-style.htm */

/* Header styles */
#head {
    clear:both;
float:left;
width:100%;
}


/* column container */
.colmask {
    position:relative;      /* This fixes the IE7 overflow hidden bug */
clear:both;
float:left;
width:100%;             /* width of whole page */
overflow:hidden;            /* This chops off any overhanging divs */
}
/* common column settings */
.colright,
.colmid,
.colleft {
    float:left;
width:100%;             /* width of page */
position:relative;
}
.col1,
.col2,
.col3 {
    float:left;
position:relative;
padding:0 0 1em 0;      /* no left and right padding on columns, we just make them narrower instead
                            only padding top and bottom is included here, make it whatever value you need */
overflow:hidden;
}
/* 3 Column blog style settings */
.blogstyle {
    background:#ffffff;            /* right column background colour */
}
.blogstyle .colmid {
    right:25%;              /* width of the right column */
background:#ffffff;     /* center column background colour */
}
.blogstyle .colleft {
    right:25%;              /* width of the middle column */
background:#ffffff;            /* left column background colour */
}
.blogstyle .col1 {
    width:29%;              /* width of center column content (column width minus padding on either side) */
left:52%;               /* 100% plus left padding of center column */
}
.blogstyle .col2 {
    width:29%;              /* Width of left column content (column width minus padding on either side) */
left:56%;               /* width of (right column) plus (center column left and right padding) plus (left column left padding) */
background:#eee;
}
.blogstyle .col3 {
    width:29%;              /* Width of right column content (column width minus padding on either side) */
left:60%;               /* Please make note of the brackets here:
                            (100% - left column width) plus (center column left and right padding) plus (left column left and right padding) plus (right column left padding) */
}

/* Footer styles */
#foot {
    clear:both;
float:left;
width:100%;
border-top:1px none #000;
}

</style>

<script type="text/javascript">

/*
    jQuery(document).ready(function(){
        if (jQuery('#sub-all-tracks').attr('href')) {
            // table has been removed...
            jQuery('#sub-gbrowse-table').append('<div><a href="' + jQuery('#sub-all-tracks').attr('href') + '" TARGET=_BLANK><img title="GBrowse" style="border: 1px solid black;" src="' + gb_link.replace(/gbrowse/, "gbrowse_img") + '"></a></div>');
        }
    });
 */

</script>



<div class="collection-table column-border">
<h3>External Links</h3>

<table width="100%">
<thead>
<tr>
<th>GBrowse Tracks</th>
<th>Data Files</th>
<th>Database Records</th>
</tr>
</thead>
<tbody>
<tr>
<td>
<%-- GBROWSE --%>
<c:choose>
<c:when test="${!empty subTracks}">
<p style="padding-left:20px;">View individual track:</p>
<div style="padding:5px 35px;">
<c:forEach var="track" items="${subTracks}" varStatus="track_status">
<mm:singleTrack track="${track}"/>
<br>
</c:forEach>
</div>
<p style="padding-left:20px;">View all tracks:</p>
<div style="padding:5px 35px;">
<mm:allTracks tracks="${subTracks}" dccId="${object.dCCid}"/>
</div>
</c:when>
<c:otherwise>
<p align="middle"><i>no tracks available</i></p>
</c:otherwise>
</c:choose>
</td>
<td>
<%-- FILES --%>
<c:if test="${!empty files}">
<div class="filelink" style="padding-left:20px;">
<mm:dataFiles files="${files}" dccId="${object.dCCid}"/>
</div>
</c:if>

<%-- TARBALL --%>
<div style="padding:2px 20px;">
<b>
<mm:getTarball dccId="${object.dCCid}"/>
</b>
</div>
</td>
<td>

<div class="col3">
<!-- Column 3 start -->
<!-- Column 3 end -->
</div>


</td>
</tr>
</tbody>
</table>


</div>






<div id="foot"></div>

<script type="text/javascript">

jQuery(".col3").empty();
//     jQuery(".col3").append("<h2 align='middle'>Database records</h2>");

var dbRecordsJSON;
if ('${dbRecordsJSON}' == "") {
    jQuery(".col3").append("<p align='middle'><i>no records available</i></p>");
} else {
    dbRecordsJSON = jQuery.parseJSON('${dbRecordsJSON}');

    for(var i in dbRecordsJSON){

        var size = dbRecordsJSON[i].dbRecords.length;
        var dbName = dbRecordsJSON[i].dbName;
        var dbRecords = dbRecordsJSON[i].dbRecords;

        var html = "<p style='padding: 5px 30px;'><b>" + dbName + "</b></p>";
        html = html + "<span id='" + dbName.replace(/\s+/g, "-") + "-records'>";

        if (size > 3) {
            for (var j=0;j<3;j++){
                html = html + "<span style='padding-left:50px;'>" + dbRecords[j] + "</span><br>";
            }

            html = html + "<span class='fakelink' style='padding-left:50px;' onclick='expand_external_links(\"" + dbName.replace(/\s+/g, "-") + "-records" + "\");'>... display all " + size + " records</span><br>";

        } else {
            for (var j in dbRecords){
                html = html + "<span style='padding-left:50px;'>" + dbRecords[j] + "</span><br>";
            }
        }

        html = html + "</span>";
        jQuery(".col3").append(html);
    }
}

function expand_external_links(spanid) {
    var html = "";

    for (var i in dbRecordsJSON){
        var size = dbRecordsJSON[i].dbRecords.length;
        var dbName = dbRecordsJSON[i].dbName;
        var dbRecords = dbRecordsJSON[i].dbRecords;

        if (dbName.replace(/\s+/g, "-") + "-records" == spanid) {
            for (var j in dbRecords){
                html = html + "<span style='padding-left:50px;'>" + dbRecords[j] + "</span><br>";
            }
            html = html + "<span class='fakelink' style='padding-left:50px;' onclick='collapse_external_links(\"" + spanid + "\");'>[collapse]</span>";
        }
    }

    jQuery("#" + spanid).html(html);
}

function collapse_external_links(spanid) {
    var html = "";

    for (var i in dbRecordsJSON){
        var size = dbRecordsJSON[i].dbRecords.length;
        var dbName = dbRecordsJSON[i].dbName;
        var dbRecords = dbRecordsJSON[i].dbRecords;

        if (dbName.replace(/\s+/g, "-") + "-records" == spanid) {
            for (var j=0;j<3;j++){
                html = html + "<span style='padding-left:50px;'>" + dbRecords[j] + "</span><br>";
            }
            html = html + "<span class='fakelink' style='padding-left:50px;' onclick='expand_external_links(\"" + spanid + "\");'>... display all " + size + " records</span>";
        }
    }

    jQuery("#" + spanid).html(html);
}

</script>

<!-- /submissionExternalLinksDisplayer.jsp -->