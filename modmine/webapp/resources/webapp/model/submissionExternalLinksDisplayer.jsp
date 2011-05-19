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

<h3>External Links</h3>

<div id="head" style="padding-top:5px;"></div>

<div class="colmask blogstyle">
    <div class="colmid">
        <div class="colleft">
            <div class="col1">
                <!-- Column 1 start -->
                <h2 align="middle">GBrowse tracks</h2>

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
                <!-- Column 1 end -->
            </div>
            <div class="col2">
                <!-- Column 2 start -->
                <h2 align="middle">Data files</h2>
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
                <!-- Column 2 end -->
            </div>
            <div class="col3">
                <!-- Column 3 start -->
                <h2 align="middle">Database records</h2>
                <c:choose>
                    <c:when test="${!empty dbRecords}">
                         <c:forEach var="db" items="${dbRecords}" varStatus="status">
                             <p style="padding: 5px 30px;"><b>${db.key}</b></p>
                             <c:set var="doneLoop" value="false"/>
                             <c:forEach var="record" items="${db.value}" varStatus="status">
                                 <c:if test="${not doneLoop}">
                                    <span style="padding-left:50px;">${record}</span>
                                    <br>
                                    <c:if test="${record == 'To be confirmed'}">
                                        <c:set var="doneLoop" value="true"/>
                                    </c:if>
                                 </c:if>
                             </c:forEach>
                             <br>
                         </c:forEach>
                    </c:when>
                    <c:otherwise>
                        <p align="middle"><i>no records available</i></p>
                    </c:otherwise>
                </c:choose>
                <!-- Column 3 end -->
            </div>
        </div>
    </div>
</div>

<div id="foot"></div>

<!-- /submissionExternalLinksDisplayer.jsp -->