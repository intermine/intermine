<%--
  - Author: Fengyuan Hu
  - Date: 11-06-2010
  - Copyright Notice:
  - @(#)  Copyright (C) 2002-2010 FlyMine

          This code may be freely distributed and modified under the
          terms of the GNU Lesser General Public Licence.  This should
          be distributed with the code.  See the LICENSE file for more
          information or http://www.gnu.org/copyleft/lesser.html.

  - Description: In this page, it displays the results of overlapping
                 located sequence features with the constrains and spans
                 by users'setup
  --%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<!--  spanUploadResults.jsp -->

<html:xhtml />

<link href="model/jquery_contextMenu/jquery.contextMenu.css" rel="stylesheet" type="text/css" />
<script type="text/javascript" src="model/jquery_contextMenu/jquery.contextMenu.js"></script>
<script type="text/javascript" src="model/jquery_expander/jquery.expander.js"></script>

<script type="text/javascript" class="source">

    jQuery(document).ready(function(){
       if (jQuery("#firstLine").html()==null) {
         jQuery("#resultDiv").addClass("altmessage").html("<br>No results found.<p>But if you tell us what you want to find out, maybe we can help!  Send us your question using the contact form at the bottom of the page.</p><br>");
       }

       // for contextMenu
       jQuery(".exportMenu").hide();

       // Dynamically change assign the id to contextMenu ul
       jQuery(".exportDiv").mouseover(function(e) {
         jQuery('.contextMenu').removeAttr('id');
         jQuery(e.target).parent().children(".contextMenu").attr('id', 'exportMenu');
       });

       jQuery(".exportDiv").contextMenu({ menu: 'exportMenu', leftButton: true },
         function(action, el, pos) { window.open(action, '_self');
       });

       // init pageSize in the drop down list
       jQuery("#pageSizeList").val(${pageSize});

       // expander
       jQuery("#spanWithNoFt").expander({
           expandText: 'view all'
       });
   });

    function exporta(formId, format) {
      // if form is "exportForm_all"
      if (formId.split("_")[1] == "all") {
            if (jQuery("#" + formId).html() == "") {
              jQuery("#" + formId).append('<input type="hidden" value="' + format + '" name="format" />');
            } else {
              jQuery("#" + formId).find('input[name=format]').val(format);
            }
      } else {
        jQuery("#" + formId).find('input[name=format]').val(format);
      }

      jQuery("#" + formId).submit();
    }

    function changePageSize() {
      var pagesize = jQuery('#pageSizeList').val();
      // an easy way to handle page number
      url = "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}/spanDisplayAction.do?dataId=${dataId}&page=1&pageSize=" + pagesize;
      document.location.href = url;
    }

</script>

<style type="text/css">
#selectionInfo.information {
  background-color:#E6F7FE;
  border:1px solid #BEDCE4;
}

#selectionInfo {
  background-position:10px 6px;
  background-repeat:no-repeat;
  font-size:13px;
  margin:10px 10px 0;
  padding:6px 6px 6px 0;
}

#selectionInfo table td {
  font-size:13px;
}

#spanWithNoFt a {
  float:none;
}
</style>

<%-- Error Messages --%>
<c:if test="${!empty errorMsg}">
    <div id="errorMsg" class="topBar errors" style="padding-left:34px;">
        <a href="#" onclick="javascript:jQuery('#errorMsgs').hide('slow');return false">Hide</a>
        ${errorMsg}<br>
    </div>
</c:if>

<%-- Information about ... --%>
<c:if test="${(!empty selectedFt)&&(!empty selectedExp)}">
    <div id="selectionInfo" class="information">
        <table cellspacing="0" cellpadding="0" border="0" width="100%">
        <tbody><tr>
          <td width="30px" valign="middle"><img border="0" width="16px" height="16px" src="images/icons/information.png" style="padding:10px;"></td>
          <td valign="middle">
          <b>Selected feature types: </b>${selectedFt}<br>
          <b>Selected experiments: </b>${selectedExp}<br>
          </td>
          <td align="right" valign="middle">
              <a onclick="javascript:jQuery('#selectionInfo').hide('slow');return false" href="#">Hide</a>
          </td>
        </tr>
        </tbody></table>
    </div>
</c:if>

<%-- Extra information about spans without overlap features --%>
<c:if test="${!empty spanWithNoFt}">
    <div id="extraInfo" class="topBar errors" style="padding-left:34px;">
      <a href="#" onclick="javascript:jQuery('#extraInfo').hide('slow');return false">Hide</a>
      <span id="spanWithNoFt">
        ${spanWithNoFt}
      </span>
      <br>
    </div>
</c:if>

<div id="resultDiv" align="left" style="font-size:0.8em; padding-top:10px;">
    <%-- Pagination --%>
    <div style="float: right; margin-right: 35px;">Page size
    <select id="pageSizeList" onchange="changePageSize()" name="pageSize">
        <option value="10">10</option>
        <option value="25">25</option>
        <option value="50">50</option>
        <option value="100">100</option>
    </select>
    <c:choose>
      <c:when test="${totalPage <= 1}">
        <span style="color:grey;"><span style="font-family:'Comic Sans MS';"><<</span> First </span>
        <span style="color:grey;"><span style="font-family:'Comic Sans MS';"><</span> Prev </span>
        <span style="color:grey;">|</span>
        <span style="color:grey;"> Next <span style="font-family:'Comic Sans MS';">></span></span>
        <span style="color:grey;"> Last <span style="font-family:'Comic Sans MS';">>></span></span>
      </c:when>
      <c:otherwise>
        <c:choose>
          <c:when test="${currentPage <= 1}">
            <span style="color:grey;"><span style="font-family:'Comic Sans MS';"><<</span> First </span>
            <span style="color:grey;"><span style="font-family:'Comic Sans MS';"><</span> Prev </span>
            <span style="color:grey;">|</span>
            <a href="${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}/spanDisplayAction.do?dataId=${dataId}&page=${currentPage+1}&pageSize=${currentPageSize}"> Next <span style="font-family:'Comic Sans MS';">></span></a>
            <a href="${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}/spanDisplayAction.do?dataId=${dataId}&method=last&pageSize=${currentPageSize}"> Last <span style="font-family:'Comic Sans MS';">>></span></a>
          </c:when>
          <c:when test="${currentPage >= totalPage}">
            <a href="${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}/spanDisplayAction.do?dataId=${dataId}&method=first&pageSize=${currentPageSize}"><span style="font-family:'Comic Sans MS';"><<</span> First</a>
            <a href="${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}/spanDisplayAction.do?dataId=${dataId}&page=${currentPage-1}&pageSize=${currentPageSize}"><span style="font-family:'Comic Sans MS';"><</span> Prev</a>
            <span style="color:grey;">|</span>
            <span style="color:grey;"> Next <span style="font-family:'Comic Sans MS';">></span></span>
            <span style="color:grey;"> Last <span style="font-family:'Comic Sans MS';">>></span></span>
          </c:when>
          <c:otherwise>
            <a href="${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}/spanDisplayAction.do?dataId=${dataId}&method=first&pageSize=${currentPageSize}"><span style="font-family:'Comic Sans MS';"><<</span> First</a>
            <a href="${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}/spanDisplayAction.do?dataId=${dataId}&page=${currentPage-1}&pageSize=${currentPageSize}"><span style="font-family:'Comic Sans MS';"><</span> Prev</a>
            <span style="color:grey;">|</span>
            <a href="${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}/spanDisplayAction.do?dataId=${dataId}&page=${currentPage+1}&pageSize=${currentPageSize}"> Next <span style="font-family:'Comic Sans MS';">></span></a>
            <a href="${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}/spanDisplayAction.do?dataId=${dataId}&method=last&pageSize=${currentPageSize}"> Last <span style="font-family:'Comic Sans MS';">>></span></a>
          </c:otherwise>
        </c:choose>
      </c:otherwise>
    </c:choose>
    </div>
    <%-- /Pagination --%>
    <%-- Export All links, refer to attributeLinkDisplayer.jsp --%>
    <div style="display: inline; padding: 0px 0px 0px 23px; line-height: 22px;">
      <c:set var="allFeaturePIDs" value=""/>
      Export all features:&nbsp;
      <a href="javascript: exporta('exportForm_all', 'tab');" class="ext_link">TAB</a>
      &nbsp;|&nbsp;
      <a href="javascript: exporta('exportForm_all', 'csv');" class="ext_link">CSV</a>
      &nbsp;|&nbsp;
      <a href="javascript: exporta('exportForm_all', 'gff3');" class="ext_link">GFF3</a>
      &nbsp;|&nbsp;
      <a href="javascript: exporta('exportForm_all', 'sequence');" class="ext_link">SEQ</a>
    </div>
    <%-- /Export links --%>
<div>
  <table id="spanResultsTable" cellpadding="0" cellspacing="0" border="0" class="dbsources" width="97%" style="min-width: 450px">
    <thead>
        <tr valign="middle">
          <th align="center">Genome Region</th>
          <th align="center">Feature</th>
          <th align="center">Feature Type</th>
          <th align="center">Location</th>
          <th align="center">Number of Matched Bases</th>
          <th align="center">Submission DCCid</th>
          <th align="center">Submission Title</th>
        </tr>
    </thead>
    <tbody>
    <c:forEach var="element" items="${paginatedResultsMap}">
        <c:if test="${!empty element.value}">
            <tr id="firstLine">
              <td valign="top" rowspan="${fn:length(element.value)}">
                <c:set var="elementKeyToString" value="${element.key.chr}:${element.key.start}..${element.key.end}" />
                <b><c:out value="${elementKeyToString}"/></b><br>

                <%-- To build Gbrowse link --%>
                <c:forEach var="trackMapItem" items="${spanTrackMap}">
                  <c:set var="trackMapItemKeyToString" value="${trackMapItem.key.chr}:${trackMapItem.key.start}..${trackMapItem.key.end}" />
                  <c:if test="${trackMapItemKeyToString == elementKeyToString}">
                    <c:set var="GBROWSE_TAIL" value=""/>
                      <c:choose>
                        <c:when test="${!empty trackMapItem.value}">
                          <c:forEach var="gbTrackList" items="${trackMapItem.value}">
                              <c:forEach var="gbTrack" items="${gbTrackList.value}">
                                <c:set var="GBROWSE_TAIL" value="${GBROWSE_TAIL}${gbTrack.track}/${gbTrack.subTrack}-"/>
                              </c:forEach>
                          </c:forEach>
                        </c:when>
                        <c:otherwise>
                        </c:otherwise>
                      </c:choose>
                  </c:if>
                </c:forEach>
                <c:choose>
                  <c:when test="${!empty GBROWSE_TAIL}">
                    <c:set var="GBROWSE_TAIL" value="${fn:substring(GBROWSE_TAIL,0,fn:length(GBROWSE_TAIL)-1)}"/>
                    <c:choose>
                      <c:when test='${spanOrg == "D. melanogaster"}'>
                        <c:set var="GBROWSE_URL" value="${GBROWSE_BASE_URL}fly/?label="/>
                        <div style="align:center">
                          <a title="Some submissions may not have tracks" target="_blank" href="${GBROWSE_URL}${GBROWSE_TAIL}">
                            GBrowse tracks
                          </a>
                          <img border="0" title="fly" src="model/images/fly_gb.png" class="arrow">
                        </div>
                      </c:when>
                      <c:when test='${spanOrg == "C. elegans"}'>
                        <c:set var="GBROWSE_URL" value="${GBROWSE_BASE_URL}worm/?label="/>
                        <div style="align:center">
                          <a title="Some submissions may not have tracks" target="_blank" href="${GBROWSE_URL}${GBROWSE_TAIL}">
                            GBrowse tracks
                          </a>
                          <img border="0" title="worm" src="model/images/worm_gb.png" class="arrow">
                        </div>
                      </c:when>
                    </c:choose>
                  </c:when>
                  <c:otherwise>
                    <c:choose>
                      <c:when test='${spanOrg == "D. melanogaster"}'>
                        <c:set var="GBROWSE_URL" value="${GBROWSE_BASE_URL}fly/?label="/>
                        <div style="align:center">
                          <span style="color:grey;" title="No tracks available">
                            GBrowse tracks
                          </span>
                          <img border="0" title="fly" src="model/images/fly_gb.png" class="arrow">
                        </div>
                      </c:when>
                      <c:when test='${spanOrg == "C. elegans"}'>
                        <c:set var="GBROWSE_URL" value="${GBROWSE_BASE_URL}worm/?label="/>
                        <div style="align:center">
                          <span style="color:grey;" title="No tracks available">
                            GBrowse tracks
                          </span>
                          <img border="0" title="worm" src="model/images/worm_gb.png" class="arrow">
                        </div>
                      </c:when>
                    </c:choose>
                  </c:otherwise>
                </c:choose>
                <%-- /To build Gbrowse link --%>

                <%-- Export links, refer to attributeLinkDisplayer.jsp --%>
                  <c:set var="featurePIDs" value=""/>
                  <c:forEach var="spanQueryResultRow" begin="0" end="${fn:length(element.value)-1}" items="${element.value}">
                    <c:set var="featurePIDs" value="${featurePIDs}${spanQueryResultRow.featurePID}, "/>
                    <c:set var="allFeaturePIDs" value="${allFeaturePIDs}${spanQueryResultRow.featurePID}, "/>
                  </c:forEach>
                  <c:set var="featurePIDs" value="${fn:substring(featurePIDs,0,fn:length(featurePIDs)-2)}"/>
                  <c:set var="formIdTail" value="${element.key.chr}_${element.key.start}_${element.key.end}" />

                  <form id="exportForm_${formIdTail}" action="features.do" method="post">
                    <input type="hidden" value="span" name="type" />
                    <input type="hidden" value="export" name="action" />
                    <input type="hidden" value="${featurePIDs}" name="value" />
                    <input type="hidden" value="${spanOrg}" name="extraValue" />
                    <input type="hidden" value="" name="format" />
                  </form>

                  <div style="align:center; padding-bottom:12px">
                    <span class="fakelink exportDiv">
                      Export data
                    </span>
                    <img class="exportDiv" style="position:relative; top:3px;" border="0" src="model/images/download.png" title="export data" height="18" width="18"/>
                    <ul class="contextMenu">
                      <li class="tab"><a href="#javascript: exporta('exportForm_${formIdTail}', 'tab');" class="ext_link">TAB</a></li>
                      <li class="csv"><a href="#javascript: exporta('exportForm_${formIdTail}', 'csv');" class="ext_link">CSV</a></li>
                      <li class="gff"><a href="#javascript: exporta('exportForm_${formIdTail}', 'gff3');" class="ext_link">GFF3</a></li>
                      <li class="seq"><a href="#javascript: exporta('exportForm_${formIdTail}', 'sequence');" class="ext_link">SEQ</a></li>
                    </ul>
                  </div>
                <%-- /Export links --%>

                <%-- Link to Galaxy --%>
                  <form id="exportGalaxy_${formIdTail}" action="exportOptions.do" method="post">
                    <input type="hidden" value="${featurePIDs}" name="value" />
                    <input type="hidden" value="galaxy" name="type" />
                  </form>
                  <div style="align:center">
                    <a href="javascript: jQuery('#exportGalaxy_${formIdTail}').submit();" class="ext_link">
                      Export to Galaxy
                    </a>
                    <img border="0" title="Export to Galaxy" src="model/images/Galaxy_logo_small.png" class="arrow" style="height:13%; width:13%">
                  </div>
                <%-- /Link to Galaxy --%>

              </td>

              <c:forEach var="spanQueryResultRow" begin="0" end="0" items="${element.value}">
                  <td>
                    <a title="${spanQueryResultRow.featurePID}" href="${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}/portal.do?externalid=${spanQueryResultRow.featurePID}&class=${fn:split(spanQueryResultRow.featureClass.name,".")[fn:length(fn:split(spanQueryResultRow.featureClass.name,"."))-1]}">
                      <c:set var="maxLength" value="50"/>
                      <im:abbreviate value="${spanQueryResultRow.featurePID}" length="${maxLength}"/>
                    </a>
                  </td>
                  <td><c:out value="${fn:split(spanQueryResultRow.featureClass.name,\".\")[fn:length(fn:split(spanQueryResultRow.featureClass.name,\".\"))-1]}"/></td>
                  <td><c:out value="${spanQueryResultRow.chr}:${spanQueryResultRow.start}..${spanQueryResultRow.end}"/></td>
                  <td>
                    <c:choose>
                      <c:when test="${spanQueryResultRow.start <= element.key.start && spanQueryResultRow.end >= element.key.start && spanQueryResultRow.end <= element.key.end}">
                        <c:out value="${spanQueryResultRow.end-element.key.start+1}"/>
                      </c:when>
                      <c:when test="${spanQueryResultRow.start >= element.key.start && spanQueryResultRow.start <= element.key.end && spanQueryResultRow.end >= element.key.end}">
                        <c:out value="${element.key.end-spanQueryResultRow.start+1}"/>
                      </c:when>
                      <c:when test="${spanQueryResultRow.start >= element.key.start && spanQueryResultRow.end <= element.key.end}">
                        <c:out value="${spanQueryResultRow.end-spanQueryResultRow.start+1}"/>
                      </c:when>
                      <c:when test="${spanQueryResultRow.start <= element.key.start && spanQueryResultRow.end >= element.key.end}">
                        <c:out value="${element.key.end-element.key.start+1}"/>
                      </c:when>
                    </c:choose>
                  </td>
                  <td><a href="${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}/portal.do?externalid=${spanQueryResultRow.subDCCid}&class=Submission"><c:out value="${spanQueryResultRow.subDCCid}"/></a></td>
                  <td>
                    <c:set var="maxLength" value="50"/>
                    <im:abbreviate value="${spanQueryResultRow.subTitle}" length="${maxLength}"/>
                  </td>
              </c:forEach>
            </tr>

            <c:forEach var="spanQueryResultRow" begin="1" end="${fn:length(element.value)-1}" items="${element.value}">
                <tr>
                  <td>
                    <a title="${spanQueryResultRow.featurePID}" href="${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}/portal.do?externalid=${spanQueryResultRow.featurePID}&class=${fn:split(spanQueryResultRow.featureClass.name,".")[fn:length(fn:split(spanQueryResultRow.featureClass.name,"."))-1]}">
                      <c:set var="maxLength" value="50"/>
                      <im:abbreviate value="${spanQueryResultRow.featurePID}" length="${maxLength}"/>
                    </a>
                  </td>
                  <td><c:out value="${fn:split(spanQueryResultRow.featureClass.name,\".\")[fn:length(fn:split(spanQueryResultRow.featureClass.name,\".\"))-1]}"/></td>
                  <td><c:out value="${spanQueryResultRow.chr}:${spanQueryResultRow.start}..${spanQueryResultRow.end}"/></td>
                  <td>
                    <c:choose>
                      <c:when test="${spanQueryResultRow.start <= element.key.start && spanQueryResultRow.end >= element.key.start && spanQueryResultRow.end <= element.key.end}">
                        <c:out value="${spanQueryResultRow.end-element.key.start+1}"/>
                      </c:when>
                      <c:when test="${spanQueryResultRow.start >= element.key.start && spanQueryResultRow.start <= element.key.end && spanQueryResultRow.end >= element.key.end}">
                        <c:out value="${element.key.end-spanQueryResultRow.start+1}"/>
                      </c:when>
                      <c:when test="${spanQueryResultRow.start >= element.key.start && spanQueryResultRow.end <= element.key.end}">
                        <c:out value="${spanQueryResultRow.end-spanQueryResultRow.start+1}"/>
                      </c:when>
                      <c:when test="${spanQueryResultRow.start <= element.key.start && spanQueryResultRow.end >= element.key.end}">
                        <c:out value="${element.key.end-element.key.start+1}"/>
                      </c:when>
                    </c:choose>
                  </td>
                  <td><a href="${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}/portal.do?externalid=${spanQueryResultRow.subDCCid}&class=Submission"><c:out value="${spanQueryResultRow.subDCCid}"/></a></td>
                  <td>
                    <c:set var="maxLength" value="50"/>
                    <im:abbreviate value="${spanQueryResultRow.subTitle}" length="${maxLength}"/>
                  </td>
                </tr>
            </c:forEach>
        </c:if>
    </c:forEach>
    <c:set var="allFeaturePIDs" value="${fn:substring(allFeaturePIDs,0,fn:length(allFeaturePIDs)-2)}"/>
    <form id="exportForm_all" action="features.do" method="post">
        <input type="hidden" value="span" name="type" />
        <input type="hidden" value="export" name="action" />
        <input type="hidden" value="${allFeaturePIDs}" name="value" />
        <input type="hidden" value="${spanOrg}" name="extraValue" />
    </form>
    </tbody>
  </table>

</div>
    <div align="center">
    <%-- Pagination at bottom --%>
    <c:choose>
      <c:when test="${totalPage <= 1}">
        <span style="color:grey;"><span style="font-family:'Comic Sans MS';"><<</span> First </span>
        <span style="color:grey;"><span style="font-family:'Comic Sans MS';"><</span> Prev </span>
        <span>1 - ${totalRecord} of ${totalRecord}</span>
        <span style="color:grey;"> Next <span style="font-family:'Comic Sans MS';">></span></span>
        <span style="color:grey;"> Last <span style="font-family:'Comic Sans MS';">>></span></span>
      </c:when>
      <c:otherwise>
        <c:choose>
          <c:when test="${currentPage <= 1}">
            <span style="color:grey;"><span style="font-family:'Comic Sans MS';"><<</span> First </span>
            <span style="color:grey;"><span style="font-family:'Comic Sans MS';"><</span> Prev </span>
            <span>1 - ${currentPage*currentPageSize} of ${totalRecord}</span>
            <a href="${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}/spanDisplayAction.do?dataId=${dataId}&page=${currentPage+1}&pageSize=${currentPageSize}"> Next <span style="font-family:'Comic Sans MS';">></span></a>
            <a href="${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}/spanDisplayAction.do?dataId=${dataId}&method=last&pageSize=${currentPageSize}"> Last <span style="font-family:'Comic Sans MS';">>></span></a>
          </c:when>
          <c:when test="${currentPage >= totalPage}">
            <a href="${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}/spanDisplayAction.do?dataId=${dataId}&method=first&pageSize=${currentPageSize}"><span style="font-family:'Comic Sans MS';"><<</span> First</a>
            <a href="${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}/spanDisplayAction.do?dataId=${dataId}&page=${currentPage-1}&pageSize=${currentPageSize}"><span style="font-family:'Comic Sans MS';"><</span> Prev</a>
            <span>${(currentPage-1)*currentPageSize+1} - ${totalRecord} of ${totalRecord}</span>
            <span style="color:grey;"> Next <span style="font-family:'Comic Sans MS';">></span></span>
            <span style="color:grey;"> Last <span style="font-family:'Comic Sans MS';">>></span></span>
          </c:when>
          <c:otherwise>
            <a href="${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}/spanDisplayAction.do?dataId=${dataId}&method=first&pageSize=${currentPageSize}"><span style="font-family:'Comic Sans MS';"><<</span> First</a>
            <a href="${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}/spanDisplayAction.do?dataId=${dataId}&page=${currentPage-1}&pageSize=${currentPageSize}"><span style="font-family:'Comic Sans MS';"><</span> Prev</a>
            <span>${(currentPage-1)*currentPageSize+1} - ${currentPage*currentPageSize} of ${totalRecord}</span>
            <a href="${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}/spanDisplayAction.do?dataId=${dataId}&page=${currentPage+1}&pageSize=${currentPageSize}"> Next <span style="font-family:'Comic Sans MS';">></span></a>
            <a href="${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}/spanDisplayAction.do?dataId=${dataId}&method=last&pageSize=${currentPageSize}"> Last <span style="font-family:'Comic Sans MS';">>></span></a>
          </c:otherwise>
        </c:choose>
      </c:otherwise>
    </c:choose>
    <%-- /Pagination at bottom --%>
    </div>
</div>

<!--  /spanUploadResults.jsp -->
