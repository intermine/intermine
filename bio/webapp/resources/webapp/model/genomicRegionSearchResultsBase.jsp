<%--
  - Author: Fengyuan Hu
  - Created: 9-May-2011
  - Description: The page to display genomic search results.
  --%>

<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<!--  genomicRegionSearchResultsBase.jsp -->
<script type="text/javascript" src="model/jquery_expander/jquery.expander.js"></script>
<script type="text/javascript" src="model/jquery_periodicalupdater/jquery.periodicalupdater.js"></script>
<script type="text/javascript" src="model/jquery_progressbar/jquery.progressbar.js"></script>
<script type="text/javascript" src="model/jquery_download/jquery.download.js"></script>
<script type="text/javascript" src="model/jquery_download/jquery.download.js"></script>
<script type="text/javascript">
    // globle variables for results javascript use
    var span_query_total_count = parseInt("${spanQueryTotalCount}");
    var webapp_path = "${WEB_PROPERTIES['webapp.path']}";
    var span_uuid_string = "${spanUUIDString}";
    var gbrowse_image_url = "${GBROWSE_IMAGE_URL}";
    var gbrowse_base_url = "${GBROWSE_BASE_URL}";
    var export_chromosome_segment = "${WEB_PROPERTIES['genomicRegionSearch.exportChromosomeSegment']}";
</script>
<script type="text/javascript" src="model/genomic_region_search/${resultsJavascript}.js"></script>
<link type="text/css" rel="stylesheet" href="model/genomic_region_search/css/${resultsCss}.css"/>

<%--
        <ul class="sf-menu">
            <li class="current">
                <a href="javascript:;">Export</a>
                <ul>
                    <li class="current">
                        <a href="javascript:;">ALL</a>
                        <ul>
                            <li><a href="javascript:;"><img border="0" width="18" height="18" src="model/images/download.png">TSV</a></li>
                            <li><a href="javascript:;">CSV</a></li>
                            <li><a href="javascript:;">GFF</a></li>
                            <li><a href="javascript:;">SEQ</a></li>
                        </ul>
                    </li>
                    <li>
                        <a href="javascript:;">Gene</a>
                        <ul>
                            <li><a href="javascript:;">TSV</a></li>
                            <li><a href="javascript:;">CSV</a></li>
                            <li><a href="javascript:;">GFF</a></li>
                            <li><a href="javascript:;">SEQ</a></li>
                        </ul>
                    </li>
                    <li>
                        <a href="javascript:;">Exon</a>
                        <ul>
                            <li><a href="javascript:;">TSV</a></li>
                            <li><a href="javascript:;">CSV</a></li>
                            <li><a href="javascript:;">GFF</a></li>
                            <li><a href="javascript:;">SEQ</a></li>
                        </ul>
                    </li>
                </ul>
            </li>
        </ul>
--%>
<%-- Error Messages --%>
<c:if test="${!empty errorMsg}">
    <div id="errorMsg" class="topBar errors" style="padding-left:34px;">
        <a href="#" onclick="javascript:jQuery('#errorMsg').hide('slow');return false">Hide</a>
        ${errorMsg}<br>
    </div>
</c:if>

<%-- liftOver status Messages --%>
<c:if test="${!empty liftOverStatus}">
    <div id="liftOverStatus" class="topBar errors" style="padding-left:34px;">
        <a href="#" onclick="javascript:jQuery('#liftOverStatus').hide('slow');return false">Hide</a>
        ${liftOverStatus}<br>
    </div>
</c:if>

<%-- User selections information --%>
<c:if test="${!empty selectionInfo}">
    <div id="selectionInfo" class="information">
        <table cellspacing="0" cellpadding="0" border="0" width="100%">
        <tbody><tr>
          <td width="30px" valign="middle"><img border="0" width="16px" height="16px" src="images/icons/information.png" style="padding:10px;"></td>
          <td valign="middle">
              <c:forEach var="sel" items="${selectionInfo}" varStatus="status">
                ${sel}
                <c:if test="${!status.last}"><br></c:if>
              </c:forEach>
          </td>
          <td align="right" valign="middle">
              <a onclick="javascript:jQuery('#selectionInfo').hide('slow');return false" href="#">Hide</a>
          </td>
        </tr>
        </tbody></table>
    </div>
</c:if>

<div style="display: none;" id="ctxHelpDiv">
  <div class="topBar info">
    <a onclick="javascript:jQuery('#ctxHelpDiv').hide('slow');return false" href="#">Close</a>
    <div id="ctxHelpTxt"></div>
  </div>
</div>

<div id="progressbar_div" align="middle">
    <table>
      <tbody>
        <tr>
          <td></td>
          <td>
            <span id="progressbar"></span>
          <td>
          <td>
            <span id="progressbar_status"></span>
          </td>
        </tr>
      </tbody>
    </table>
</div>

<div id="resultDiv" align="left" style="font-size:0.8em; padding-top:10px;">

    <%-- experiment --%>
    <%--
    <div id="region-select-div">
        <select id="region-select-list" >
            <option value="all">All Regions</option>
        </select>
    </div>

    <div id="grouping-div">
        <input text="text" id="group-input" />
        <p id="group-search">Search</p>
        <script>
            jQuery("#group-search").click(function () {
                // regular expression check
                var ddotsRegex = /^[^:\t\s]+: ?\d+\.\.\d+$/;
                var empty = /^\s*$/;

                var interval = jQuery("#group-input").val();

                if (interval.match(empty)) {
                    jQuery("#group-input").focus();
                }
                else if (!interval.match(ddotsRegex)) {
                    alert(interval + " is in an invalid format...");
                    jQuery("#group-input").focus();
                } else {
                    // ajax call
                    jQuery.post("genomicRegionSearchAjax.do", { spanUUIDString: '${spanUUIDString}', groupRegions: "true", interval: interval }, function(results){
                        jQuery("#upper-pag-div").hide();
                        jQuery("#export-all-div").hide();
                        jQuery("#bottom-pag-div").hide();
                        addResultToTable(results);
                    }, "html");
                }

            });
        </script>
    </div>
    --%>
    <%-- experiment --%>

    <c:choose>
        <c:when test="${WEB_PROPERTIES['genomicRegionSearch.jbrowse.display'] eq 'true'}">
            <div id="genome-browser-div">
                <iframe name="genome-browser" height="300px" width="98%" style="border: 1px solid #dfdfdf; padding: 1%" src="http://www.metabolicmine.org/jbrowse/"></iframe>
            </div>
        </c:when>
    </c:choose>

    <%-- Pagination --%>
    <div id="upper-pag-div" style="float: right; margin-right: 35px;">Page size
        <select id="pageSizeList" onchange="changePageSize()" name="pageSize">
            <option value="10">10</option>
            <option value="25">25</option>
            <option value="50">50</option>
            <option value="100">100</option>
        </select>

        <span id="upperNav"></span>
    </div>
    <%-- /Pagination --%>

    <%-- Export All links, refer to attributeLinkDisplayer.jsp --%>
    <div id="export-all-div"></div>
    <%-- /Export links --%>

<div>
  <table id="genomic-region-results-table" cellpadding="0" cellspacing="0" border="0" class="regions" width="97%" style="min-width: 450px">
  </table>
</div>

<div id="bottom-pag-div" align="center">
    <%-- Pagination at bottom --%>
    <span id="lowerNav"></span>
    <%-- /Pagination at bottom --%>
    </div>

</div>

<!--  /genomicRegionSearchResultsBase.jsp -->