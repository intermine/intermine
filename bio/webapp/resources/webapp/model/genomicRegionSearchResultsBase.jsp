<%--
  - Author: Fengyuan Hu
  - Created: 9-May-2012
  - Description: The page to display genomic search results.
  --%>

<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<!--  genomicRegionSearchResultsBase.jsp -->

<link href="model/jquery_contextMenu/jquery.contextMenu.css" rel="stylesheet" type="text/css" />
<link rel="stylesheet" type="text/css" href="model/jquery_superfish/css/superfish.css" media="screen">
<link type="text/css" rel="stylesheet" href="model/jquery_ui/css/smoothness/jquery-ui-1.8.13.custom.css"/>
<link type="text/css" rel="stylesheet" href="model/genomic_region_search/css/${resultsCss}.css"/>

<script type="text/javascript" src="model/jquery_contextMenu/jquery.contextMenu.js"></script>
<script type="text/javascript" src="model/jquery_expander/jquery.expander.js"></script>
<script type="text/javascript" src="model/jquery_periodicalupdater/jquery.periodicalupdater.js"></script>
<script type="text/javascript" src="model/jquery_progressbar/jquery.progressbar.js"></script>
<script type="text/javascript" src="model/jquery_download/jquery.download.js"></script>
<script type="text/javascript" src="model/jquery_superfish/js/superfish.js"></script>
<script type="text/javascript" src="model/jquery_superfish/js/hoverIntent.js"></script>
<script type="text/javascript" src="model/jquery_download/jquery.download.js"></script>
<script type="text/javascript" src="<html:rewrite page='/model/jquery_ui/jquery-ui-1.8.13.custom.min.js'/>"></script>
<script type="text/javascript" class="source">
    var is_all_queries_finished = false;
    var spanQueryTotalCount = parseInt(${spanQueryTotalCount});
    var finishedQueryCount = 0;
    var current_page_size = 10;
    var current_page_no = 1;
    var spanResultWaitingIntervalId = "";

    //jQuery(function(){
    //        jQuery('ul.sf-menu').superfish();
    //    });

    jQuery(document).ready(function(){

       if ("${noneValidGenomicRegions}"=="true") {
           jQuery("#resultDiv").addClass("altmessage").html("<br>All genomic regions are invalid.<br>");
       } else {
            init();

            // polling
            jQuery.PeriodicalUpdater("genomicRegionSearchAjax.do", {
                method: 'post',         // method; get or post
                data: {spanUUIDString: "${spanUUIDString}", getProgress: "true"}, // array of values to be passed to the page - e.g. {name: "John", greeting: "hello"}
                minTimeout: 500,          // starting value for the timeout in milliseconds
                maxTimeout: 5000,       // maximum length of time between requests
                multiplier: 2,          // if set to 2, timerInterval will double each time the response hasn't changed (up to maxTimeout)
                type: 'text',           // response type - text, xml, json, etc.  See $.ajax config options
                maxCalls: 0,            // maximum number of calls. 0 = no limit.
                autoStop: 50            // automatically stop requests after this many returns of the same data. 0 = disabled.
             }, function(data) {
                finishedQueryCount = parseInt(data);
                if (finishedQueryCount < spanQueryTotalCount) {
                    var percentage = Math.floor(100 * finishedQueryCount / spanQueryTotalCount);
                    jQuery("#progressbar").progressBar(percentage);
                    jQuery("#progressbar_status").html(finishedQueryCount + "/" + spanQueryTotalCount);
                } else { // all queries finished
                    is_all_queries_finished = true;
                    jQuery("#progressbar_div").hide();
                    displayJBrowse();
                      enableExportAll();
                      updatePageNavBarAfterQueryFinish(current_page_no, current_page_size);
                }
             });

            // Start to load the first 10 results
            loadResultData(current_page_size, current_page_no);
       }

   });

    function init() {

        // disable export all
        disableExportAll()

        // page size 10 is selected by default
        jQuery('#pageSizeList option[value=10]').attr('selected','selected');

        // page navigation
        updatePageNavBarBeforeQueryFinish();

        // progressBar init
        jQuery("#progressbar").progressBar({
                                            showText: false,
                                            boxImage: 'model/jquery_progressbar/images/progressbar.gif',
                                            barImage: {
                                                        0:  'model/jquery_progressbar/images/progressbg_red.gif',
                                                        30: 'model/jquery_progressbar/images/progressbg_orange.gif',
                                                        70: 'model/jquery_progressbar/images/progressbg_green.gif'
                                                      }
                                         });
    }

    function disableExportAll() {
        jQuery("#export-all-div").empty();
        jQuery("#export-all-div").append('Export for all regions:&nbsp;<span style="color:grey;">TAB</span>&nbsp;|&nbsp;<span style="color:grey;">CSV</span>&nbsp;|&nbsp;<span style="color:grey;">GFF3</span>&nbsp;|&nbsp;<span style="color:grey;">SEQ</span> or Create List by feature type: <select></select>');
    }

    function enableExportAll() {
        jQuery("#export-all-div").empty();

        jQuery.post("genomicRegionSearchAjax.do", { spanUUIDString: '${spanUUIDString}', isEmptyFeature: "true" }, function(isEmptyFeature){
            if (isEmptyFeature.trim() == "hasFeature") {
                jQuery.post("genomicRegionSearchAjax.do", { spanUUIDString: '${spanUUIDString}', generateCreateListHtml: "true" }, function(createListHtml){
                    jQuery("#export-all-div").append('Export for all regions:&nbsp;<a href="javascript: exportFeatures(\'all\', \'SequenceFeature\', \'tab\');" class="ext_link">TAB</a>&nbsp;|&nbsp;<a href="javascript: exportFeatures(\'all\', \'SequenceFeature\', \'csv\');" class="ext_link">CSV</a>&nbsp;|&nbsp;<a href="javascript: exportFeatures(\'all\', \'SequenceFeature\', \'gff3\');" class="ext_link">GFF3</a>&nbsp;|&nbsp;<a href="javascript: exportFeatures(\'all\', \'SequenceFeature\', \'sequence\');" class="ext_link">SEQ</a>' + createListHtml);
                });
            } else {
                jQuery("#export-all-div").append('Export for all regions:&nbsp;<span style="color:grey;">TAB</span>&nbsp;|&nbsp;<span style="color:grey;">CSV</span>&nbsp;|&nbsp;<span style="color:grey;">GFF3</span>&nbsp;|&nbsp;<span style="color:grey;">SEQ</span> or Create List by feature type: <select></select>');
            }
         });
    }

    function exportFeatures(criteria, facet, format) {
        jQuery.download("genomicRegionSearchAjax.do", "exportFeatures=true&spanUUIDString=${spanUUIDString}&criteria=" + criteria + "&facet=" + facet + "&format=" + format);
    }

    function createList(criteria, id, facet) { // id e.g. I-100-200
        if (id) { // JS will convert null, undefined, 0 and "" to bollean false
          facet = jQuery("#"+id).val();
        }

        jQuery.post("genomicRegionSearchAjax.do", { spanUUIDString: '${spanUUIDString}', createList: "true", criteria: criteria, facet: facet }, function(bagName){
            window.location.href = "/${WEB_PROPERTIES['webapp.path']}/bagDetails.do?bagName=" + bagName;
        }, "text");
    }

    function exportToGalaxy(genomicRegion) {
        jQuery.post("genomicRegionSearchAjax.do", { spanUUIDString: '${spanUUIDString}', getFeatures: "true", grString: genomicRegion }, function(featureIds){
            featureIds ='<input type="hidden" name="featureIds" value="' + featureIds + '" />';
            orgName ='<input type="hidden" name="orgName" value="' + genomicRegion.split("|")[genomicRegion.split("|").length - 1] + '" />';
            jQuery('<form action="galaxyExportOptions.do" method="post">' + featureIds + orgName + '</form>').appendTo('body').submit().remove();
        }, "text");
    }

    function changePageSize() {
      current_page_size = jQuery('#pageSizeList').val();
      current_page_no = 1;

      loadResultData(current_page_size, current_page_no);

      if(finishedQueryCount < spanQueryTotalCount){
            updatePageNavBarBeforeQueryFinish();
        } else {
            updatePageNavBarAfterQueryFinish(current_page_no, current_page_size);
        }
    }

    function loadResultData(page_size, page_num) {

        clearInterval(spanResultWaitingIntervalId);

        var from_index = (page_num - 1) * page_size; // the start index in the result map
        var to_index = page_num * page_size -1; // the end index in the result map
        if (to_index > spanQueryTotalCount)
            { to_index = spanQueryTotalCount - 1;}

        if (is_all_queries_finished == true || (finishedQueryCount - 1) > to_index) {
            paginationGetResult(from_index, to_index);
        }

        if ((finishedQueryCount - 1) < to_index) {
            spanResultWaitingIntervalId = setInterval(waitingForSpanResult, 500);
        }

        function waitingForSpanResult()
        {
          jQuery("#genomic-region-results-table > tbody").html('<img src="images/wait30.gif"/>');
          if ((finishedQueryCount - 1) >= to_index) {
              clearInterval(spanResultWaitingIntervalId);
              paginationGetResult(from_index, to_index);
          }
        }
    }

    function paginationGetResult(from_index, to_index) {

        jQuery.post("genomicRegionSearchAjax.do", { spanUUIDString: '${spanUUIDString}', getData: "true", fromIdx: from_index, toIdx: to_index }, function(results){
            addResultToTable(results);
        }, "html");
    }

    function addResultToTable(spanResult) {
        jQuery("#genomic-region-results-table").empty();
        resultToDisplay = spanResult.paginatedSpanResult;
        jQuery("#genomic-region-results-table").append(spanResult);

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
    }

    function gbrowseThumbnail(title, organism, url) {

        gb_img_url = "${GBROWSE_IMAGE_URL}"+organism+"/?" + url+";width=600;b=1";
        gb_server_url = "${GBROWSE_BASE_URL}"+organism+"/?" + url;
        jQuery("#gbrowseThumbnail").html("<a href='"+gb_server_url+"' target='_blank'><img title='GBrowse' src='"+gb_img_url+"'></a>");

        jQuery("#gbrowseThumbnail").dialog( "destroy" );
        jQuery("#gbrowseThumbnail").dialog({
                                            title: '',
                                            height: 400,
                                            width: 500,
                                            show: "fade",
                                            hide: "fade"
                                                         });

        jQuery("#gbrowseThumbnail").dialog("option", "title", title);
        // jQuery("#gbrowseThumbnail").dialog("option", "position", [50, 50]);
    }

    function updatePageNavBarAfterQueryFinish(current_page_no, current_page_size) {
        jQuery("#upperNav").empty();
        jQuery("#lowerNav").empty();

        total_page = getTotalPageNumber();
        if (total_page <= 1) {
          jQuery("#upperNav").append('<span style="color:grey;"><span style="font-family:\'Comic Sans MS\';"><<</span> First </span>')
                             .append('<span style="color:grey;"><span style="font-family:\'Comic Sans MS\';"><</span> Prev </span>')
                             .append('<span style="color:grey;">|</span>')
                             .append('<span style="color:grey;"> Next <span style="font-family:\'Comic Sans MS\';">></span></span>')
                             .append('<span style="color:grey;"> Last <span style="font-family:\'Comic Sans MS\';">>></span></span>');

          jQuery("#lowerNav").append('<span style="color:grey;"><span style="font-family:\'Comic Sans MS\';"><<</span> First </span>')
                             .append('<span style="color:grey;"><span style="font-family:\'Comic Sans MS\';"><</span> Prev </span>')
                             .append('<span style="color:grey;">1 - '+spanQueryTotalCount+' of '+spanQueryTotalCount+'</span>')
                             .append('<span style="color:grey;"> Next <span style="font-family:\'Comic Sans MS\';">></span></span>')
                             .append('<span style="color:grey;"> Last <span style="font-family:\'Comic Sans MS\';">>></span></span>');
        }
        else {
            if (current_page_no <= 1) {
                jQuery("#upperNav").append('<span style="color:grey;"><span style="font-family:\'Comic Sans MS\';"><<</span> First </span>')
                                   .append('<span style="color:grey;"><span style="font-family:\'Comic Sans MS\';"><</span> Prev </span>')
                                   .append('<span style="color:grey;">|</span>')
                                   .append('<span class="fakelink" onclick="pageNavigate(\'next\');"> Next <span style="font-family:\'Comic Sans MS\';">></span></span>')
                                   .append('<span class="fakelink" onclick="pageNavigate(\'last\');"> Last <span style="font-family:\'Comic Sans MS\';">>></span></span>');

                jQuery("#lowerNav").append('<span style="color:grey;"><span style="font-family:\'Comic Sans MS\';"><<</span> First </span>')
                                   .append('<span style="color:grey;"><span style="font-family:\'Comic Sans MS\';"><</span> Prev </span>')
                                   .append('<span style="color:grey;">1 - '+current_page_no*current_page_size+' of '+spanQueryTotalCount+'</span>')
                                   .append('<span class="fakelink" onclick="pageNavigate(\'next\');"> Next <span style="font-family:\'Comic Sans MS\';">></span></span>')
                                   .append('<span class="fakelink" onclick="pageNavigate(\'last\');"> Last <span style="font-family:\'Comic Sans MS\';">>></span></span>');
            }
            else if (current_page_no >= total_page) {
                jQuery("#upperNav").append('<span class="fakelink" onclick="pageNavigate(\'first\');"><span style="font-family:\'Comic Sans MS\';"><<</span> First </span>')
                                   .append('<span class="fakelink" onclick="pageNavigate(\'prev\');"><span style="font-family:\'Comic Sans MS\';"><</span> Prev </span>')
                                   .append('<span style="color:grey;">|</span>')
                                   .append('<span style="color:grey;"> Next <span style="font-family:\'Comic Sans MS\';">></span></span>')
                                   .append('<span style="color:grey;"> Last <span style="font-family:\'Comic Sans MS\';">>></span></span>');

                jQuery("#lowerNav").append('<span class="fakelink" onclick="pageNavigate(\'first\');"><span style="font-family:\'Comic Sans MS\';"><<</span> First </span>')
                                   .append('<span class="fakelink" onclick="pageNavigate(\'prev\');"><span style="font-family:\'Comic Sans MS\';"><</span> Prev </span>')
                                   .append('<span style="color:grey;">'+((current_page_no-1)*current_page_size+1)+' - '+spanQueryTotalCount+' of '+spanQueryTotalCount+'</span>')
                                   .append('<span style="color:grey;"> Next <span style="font-family:\'Comic Sans MS\';">></span></span>')
                                   .append('<span style="color:grey;"> Last <span style="font-family:\'Comic Sans MS\';">>></span></span>');
            }
            else {
                jQuery("#upperNav").append('<span class="fakelink" onclick="pageNavigate(\'first\');"><span style="font-family:\'Comic Sans MS\';"><<</span> First </span>')
                                   .append('<span class="fakelink" onclick="pageNavigate(\'prev\');"><span style="font-family:\'Comic Sans MS\';"><</span> Prev </span>')
                                   .append('<span style="color:grey;">|</span>')
                                   .append('<span class="fakelink" onclick="pageNavigate(\'next\');"> Next <span style="font-family:\'Comic Sans MS\';">></span></span>')
                                   .append('<span class="fakelink" onclick="pageNavigate(\'last\');"> Last <span style="font-family:\'Comic Sans MS\';">>></span></span>');

                jQuery("#lowerNav").append('<span class="fakelink" onclick="pageNavigate(\'first\');"><span style="font-family:\'Comic Sans MS\';"><<</span> First </span>')
                                   .append('<span class="fakelink" onclick="pageNavigate(\'prev\');"><span style="font-family:\'Comic Sans MS\';"><</span> Prev </span>')
                                   .append('<span style="color:grey;">'+((current_page_no-1)*current_page_size+1)+' - '+current_page_no*current_page_size+' of '+spanQueryTotalCount+'</span>')
                                   .append('<span class="fakelink" onclick="pageNavigate(\'next\');"> Next <span style="font-family:\'Comic Sans MS\';">></span></span>')
                                   .append('<span class="fakelink" onclick="pageNavigate(\'last\');"> Last <span style="font-family:\'Comic Sans MS\';">>></span></span>');
            }
        }
    }

    function updatePageNavBarBeforeQueryFinish() {
        jQuery("#upperNav").empty();
        jQuery("#lowerNav").empty();

        total_page = getTotalPageNumber();
        if (total_page <= 1) {
          jQuery("#upperNav").append('<span style="color:grey;"><span style="font-family:\'Comic Sans MS\';"><<</span> First </span>')
                             .append('<span style="color:grey;"><span style="font-family:\'Comic Sans MS\';"><</span> Prev </span>')
                             .append('<span style="color:grey;">|</span>')
                             .append('<span style="color:grey;"> Next <span style="font-family:\'Comic Sans MS\';">></span></span>')
                             .append('<span style="color:grey;"> Last <span style="font-family:\'Comic Sans MS\';">>></span></span>');

          jQuery("#lowerNav").append('<span style="color:grey;"><span style="font-family:\'Comic Sans MS\';"><<</span> First </span>')
                             .append('<span style="color:grey;"><span style="font-family:\'Comic Sans MS\';"><</span> Prev </span>')
                             .append('<span style="color:grey;">1 - '+spanQueryTotalCount+' of '+spanQueryTotalCount+'</span>')
                             .append('<span style="color:grey;"> Next <span style="font-family:\'Comic Sans MS\';">></span></span>')
                             .append('<span style="color:grey;"> Last <span style="font-family:\'Comic Sans MS\';">>></span></span>');
        }
        else {
            if (current_page_no <= 1) {
                jQuery("#upperNav").append('<span style="color:grey;"><span style="font-family:\'Comic Sans MS\';"><<</span> First </span>')
                                   .append('<span style="color:grey;"><span style="font-family:\'Comic Sans MS\';"><</span> Prev </span>')
                                   .append('<span style="color:grey;">|</span>')
                                   .append('<span class="fakelink" onclick="pageNavigate(\'next\');"> Next <span style="font-family:\'Comic Sans MS\';">></span></span>')
                                   .append('<span style="color:grey;"> Last <span style="font-family:\'Comic Sans MS\';">>></span></span>');

                jQuery("#lowerNav").append('<span style="color:grey;"><span style="font-family:\'Comic Sans MS\';"><<</span> First </span>')
                                   .append('<span style="color:grey;"><span style="font-family:\'Comic Sans MS\';"><</span> Prev </span>')
                                   .append('<span style="color:grey;">1 - '+current_page_no*current_page_size+' of '+spanQueryTotalCount+'</span>')
                                   .append('<span class="fakelink" onclick="pageNavigate(\'next\');"> Next <span style="font-family:\'Comic Sans MS\';">></span></span>')
                                   .append('<span style="color:grey;"> Last <span style="font-family:\'Comic Sans MS\';">>></span></span>');
            }
            else if (current_page_no >= total_page) {
                jQuery("#upperNav").append('<span class="fakelink" onclick="pageNavigate(\'first\');"><span style="font-family:\'Comic Sans MS\';"><<</span> First </span>')
                                   .append('<span class="fakelink" onclick="pageNavigate(\'prev\');"><span sty<img border="0" width="18" height="18" title="export data" src="model/images/download.png" style="position: relative; top: 3px;" class="exportDiv">le="font-family:\'Comic Sans MS\';"><</span> Prev </span>')
                                   .append('<span style="color:grey;">|</span>')
                                   .append('<span style="color:grey;"> Next <span style="font-family:\'Comic Sans MS\';">></span></span>')
                                   .append('<span style="color:grey;"> Last <span style="font-family:\'Comic Sans MS\';">>></span></span>');

                jQuery("#lowerNav").append('<span class="fakelink" onclick="pageNavigate(\'first\');"><span style="font-family:\'Comic Sans MS\';"><<</span> First </span>')
                                   .append('<span class="fakelink" onclick="pageNavigate(\'prev\');"><span style="font-family:\'Comic Sans MS\';"><</span> Prev </span>')
                                   .append('<span style="color:grey;">'+((current_page_no-1)*current_page_size+1)+' - '+spanQueryTotalCount+' of '+spanQueryTotalCount+'</span>')
                                   .append('<span style="color:grey;"> Next <span style="font-family:\'Comic Sans MS\';">></span></span>')
                                   .append('<span style="color:grey;"> Last <span style="font-family:\'Comic Sans MS\';">>></span></span>');
            }
            else {
                jQuery("#upperNav").append('<span class="fakelink" onclick="pageNavigate(\'first\');"><span style="font-family:\'Comic Sans MS\';"><<</span> First </span>')
                                   .append('<span class="fakelink" onclick="pageNavigate(\'prev\');"><span style="font-family:\'Comic Sans MS\';"><</span> Prev </span>')
                                   .append('<span style="color:grey;">|</span>')
                                   .append('<span class="fakelink" onclick="pageNavigate(\'next\');"> Next <span style="font-family:\'Comic Sans MS\';">></span></span>')
                                   .append('<span style="color:grey;"> Last <span style="font-family:\'Comic Sans MS\';">>></span></span>');

                jQuery("#lowerNav").append('<span class="fakelink" onclick="pageNavigate(\'first\');"><span style="font-family:\'Comic Sans MS\';"><<</span> First </span>')
                                   .append('<span class="fakelink" onclick="pageNavigate(\'prev\');"><span style="font-family:\'Comic Sans MS\';"><</span> Prev </span>')
                                   .append('<span style="color:grey;">'+((current_page_no-1)*current_page_size+1)+' - '+current_page_no*current_page_size+' of '+spanQueryTotalCount+'</span>')
                                   .append('<span class="fakelink" onclick="pageNavigate(\'next\');"> Next <span style="font-family:\'Comic Sans MS\';">></span></span>')
                                   .append('<span style="color:grey;"> Last <span style="font-family:\'Comic Sans MS\';">>></span></span>');
            }
        }
    }

    function pageNavigate(method) {

        if (method == "first") { current_page_no = 1; }
        if (method == "last") { current_page_no = getTotalPageNumber(); }
        if (method == "prev") { current_page_no--; }
        if (method == "next") { current_page_no++; }

        loadResultData(current_page_size, current_page_no);

        if(finishedQueryCount < spanQueryTotalCount){
            updatePageNavBarBeforeQueryFinish();
        } else {
            updatePageNavBarAfterQueryFinish(current_page_no, current_page_size);
        }
    }

    function getTotalPageNumber() {
        total_page = Math.floor(spanQueryTotalCount / current_page_size);
        if (spanQueryTotalCount % current_page_size != 0) { total_page++; }

        return total_page;
    }

    function displayJBrowse() {
      jQuery.post("genomicRegionSearchAjax.do", { spanUUIDString: '${spanUUIDString}', getDropDownList: "true" }, function(data){
          var regions = data.split(",");

          jQuery.each(regions, function(index, value) {
              bits = value.split("|"); // e.g. 2L:14615455..14619002|0|D. melanogaster
              jQuery('#region-select-list').append( new Option(bits[0],jQuery.trim(value)) );

          });

          jQuery("#region-select-list").change(function () {
            var selectedRegion = jQuery(this).val();

            // change JBrowse
            // mock up:
            var url = "http://www.metabolicmine.org/jbrowse?loc=Homo_sapiens_chr_3:12328867..12475843&tracks=Gene%20Track,mRNA%20Track,%20SNPs"
            window.open(url, 'jbrowse'); // open in iframe id = jbrowse

            // change results view
            if (selectedRegion != "all") {
                jQuery.post("genomicRegionSearchAjax.do", { spanUUIDString: '${spanUUIDString}', getGivenRegionsResults: "true", regions: selectedRegion }, function(results){
                    jQuery("#upper-pag-div").hide();
                    jQuery("#export-all-div").hide();
                    jQuery("#bottom-pag-div").hide();
                    addResultToTable(results);
                }, "html");
            } else {
                updatePageNavBarAfterQueryFinish(1, 10);
                jQuery("#upper-pag-div").show();
                jQuery("#export-all-div").show();
                jQuery("#bottom-pag-div").show();
                loadResultData(10, 1);
            }
          }); // add .change() at the tail will trigger on load

        }, "text");
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

#selectionInfo table td {font-size:13px;}

#spanWithNoFt a {float:none;}

img.tinyQuestionMark {
  padding-bottom:4px;
  padding-left:3px;
}

</style>
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
    <div id="export-all-div" style="display: inline; padding: 0px 0px 0px 23px; line-height: 22px;">
    </div>
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