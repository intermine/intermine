<%--
  - Author: Fengyuan Hu
  - Date: 11-06-2010
  - Copyright Notice:
  - Description: In this page, it displays the results of overlapping
                 located sequence features with the constrains and spans
                 by users'setup
  --%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!--  spanUploadResults.jsp -->

<html:xhtml />

<link href="model/jquery_contextMenu/jquery.contextMenu.css" rel="stylesheet" type="text/css" />
<link type="text/css" rel="stylesheet" href="model/jquery_ui/css/cupertino/jquery-ui-1.8.13.custom.css"/>
<script type="text/javascript" src="model/jquery_contextMenu/jquery.contextMenu.js"></script>
<script type="text/javascript" src="model/jquery_expander/jquery.expander.js"></script>
<script type="text/javascript" src="model/jquery_periodicalupdater/jquery.periodicalupdater.js"></script>
<script type="text/javascript" src="model/jquery_progressbar/jquery.progressbar.js"></script>
<script type="text/javascript" src="model/jquery_download/jquery.download.js"></script>
<script type="text/javascript" src="<html:rewrite page='/model/jquery_ui/jquery-ui-1.8.13.custom.min.js'/>"></script>

<script type="text/javascript" class="source">

    var is_all_queries_finished = false;
    var spanQueryTotalCount = parseInt(${spanQueryTotalCount});
    var finishedQueryCount = 0;
    var current_page_size = 10;
    var current_page_no = 1;
    var spanResultWaitingIntervalId = "";

    jQuery(document).ready(function(){

       if ("${SpanAllWrong}"=="true") {
           jQuery("#resultDiv").addClass("altmessage").html("<br>No results found.<p>But if you tell us what you want to find out, maybe we can help!  Send us your question using the contact form at the bottom of the page.</p><br>");
       } else {
           init();

            // polling
            jQuery.PeriodicalUpdater("spanUploadAjax.do", {
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
                } else {
                    is_all_queries_finished = true;
                    jQuery("#progressbar_div").hide();
                    enableExportAll();
                    updatePageNavBarAfterQueryFinish();
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
        jQuery("#exportAll").empty();
        jQuery("#exportAll").append('Export all features:&nbsp;<span style="color:grey;">TAB</span>&nbsp;|&nbsp;<span style="color:grey;">CSV</span>&nbsp;|&nbsp;<span style="color:grey;">GFF3</span>&nbsp;|&nbsp;<span style="color:grey;">SEQ</span>');
    }

    function enableExportAll() {
        jQuery("#exportAll").empty();

        jQuery.post("spanUploadAjax.do", { spanUUIDString: '${spanUUIDString}', isEmptyFeature: "true" }, function(isEmptyFeature){
            if (isEmptyFeature.trim() == "hasFeature") {
                jQuery("#exportAll").append('Export all features:&nbsp;<a href="javascript: exportFeatures(\'all\', \'tab\');" class="ext_link">TAB</a>&nbsp;|&nbsp;<a href="javascript: exportFeatures(\'all\', \'csv\');" class="ext_link">CSV</a>&nbsp;|&nbsp;<a href="javascript: exportFeatures(\'all\', \'gff3\');" class="ext_link">GFF3</a>&nbsp;|&nbsp;<a href="javascript: exportFeatures(\'all\', \'sequence\');" class="ext_link">SEQ</a>');
            } else {
                jQuery("#exportAll").append('Export all features:&nbsp;<span style="color:grey;">TAB</span>&nbsp;|&nbsp;<span style="color:grey;">CSV</span>&nbsp;|&nbsp;<span style="color:grey;">GFF3</span>&nbsp;|&nbsp;<span style="color:grey;">SEQ</span>');
            }
         });
    }

    function exportFeatures(criteria, format) {
        jQuery.download("features.do", "type=span&action=export&spanUUIDString=${spanUUIDString}&criteria=" + criteria + "&format=" + format);
    }

    function exportToGalaxy(span) {
        jQuery.post("spanUploadAjax.do", { spanUUIDString: '${spanUUIDString}', getFeatures: "true", spanString: span }, function(featurePids){
            inputs ='<input type="hidden" name="type" value="galaxy" /><input type="hidden" name="featureIds" value="' + featurePids + '" />';
            jQuery('<form action="galaxyExportOptions.do" method="post">'+inputs+'</form>').appendTo('body').submit().remove();
        }, "text");
    }

    function changePageSize() {
      current_page_size = jQuery('#pageSizeList').val();
      current_page_no = 1;

      loadResultData(current_page_size, current_page_no);

      if(finishedQueryCount < spanQueryTotalCount){
            updatePageNavBarBeforeQueryFinish();
        } else {
            updatePageNavBarAfterQueryFinish();
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
          jQuery("#spanResultsTable > tbody").html('<img src="images/wait30.gif"/>');
          if ((finishedQueryCount - 1) >= to_index) {
              clearInterval(spanResultWaitingIntervalId);
              paginationGetResult(from_index, to_index);
          }
        }
    }

    function paginationGetResult(from_index, to_index) {

        jQuery.post("spanUploadAjax.do", { spanUUIDString: '${spanUUIDString}', getData: "true", fromIdx: from_index, toIdx: to_index }, function(spanResult){
            addResultToTable(spanResult);
        }, "json");
    }

    function addResultToTable(spanResult) {
        jQuery("#spanResultsTable > tbody").empty();
        resultToDisplay = spanResult.paginatedSpanResult;
        for (i=0; i<resultToDisplay.length; i++) {
            features = resultToDisplay[i].features;
            if (features == null) {
                jQuery("#spanResultsTable > tbody").append("<tr><td><b>"+resultToDisplay[i].span+"</b></td><td colspan='6'><i>No overlap features found</i></td></tr>");
            } else {
                if (resultToDisplay[i].gbrowseurl != null) {
                    if(resultToDisplay[i].organism == "D. melanogaster") {
                        if (features[0].featurePId.length > 50 && features[0].subTitle.length > 50) {
                            jQuery("#spanResultsTable > tbody").append("<tr><td valign='top' rowspan='"+features.length+"'><b>"+resultToDisplay[i].span+"</b><br><div class='fakelink' style='padding: 5px 0 5px 0px;' onclick='gbrowseThumbnail(\""+resultToDisplay[i].span+"\",\"fly\", \""+resultToDisplay[i].gbrowseurl+"\")'>GBrowse thumbnail</div><div style='align:center'><a title='View GBrowse tracks on modENCODE GBrowse server' target='_blank' href='${GBROWSE_BASE_URL}fly/?"+resultToDisplay[i].gbrowseurl+"'>GBrowse tracks<img border='0' title='fly' src='model/images/fly_gb.png' class='arrow'></a></div><div style='align:center; padding-bottom:12px'><span class='fakelink exportDiv'> Export data </span><img class='exportDiv' style='position:relative; top:3px;' border='0' src='model/images/download.png' title='export data' height='18' width='18'/><ul class='contextMenu'><li class='tab'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"tab\");' class='ext_link'>TAB</a></li><li class='csv'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"csv\");' class='ext_link'>CSV</a></li><li class='gff'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"gff3\");' class='ext_link'>GFF3</a></li><li class='seq'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"sequence\");' class='ext_link'>SEQ</a></li></ul></div><div style='align:center'><a href='javascript: exportToGalaxy(\""+resultToDisplay[i].span+"\");' class='ext_link'> Export to Galaxy <img border='0' title='Export to Galaxy' src='model/images/Galaxy_logo_small.png' class='arrow' style='height:13%; width:13%'></a></div></td><td><a target='_blank' title='" + features[0].featurePId + "' href='" + "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}" + "/portal.do?externalid=" + features[0].featurePId + "&class=" + features[0].featureType + "'>" + features[0].featurePId.substring(0,50) + "...</a></td><td>"+features[0].featureType+"</td><td>"+features[0].location+"</td><td>"+features[0].machedBaseCount+"</td><td><a target='_blank' href='" + "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}" + "/portal.do?externalid=" + features[0].dccId + "&class=Submission'>" + features[0].dccId + "</a></td><td><span title='" + features[0].subTitle + "'>" + features[0].subTitle.substring(0,50) + "...</span></td></tr>");
                        }

                        if (features[0].featurePId.length > 50 && features[0].subTitle.length <= 50) {
                            jQuery("#spanResultsTable > tbody").append("<tr><td valign='top' rowspan='"+features.length+"'><b>"+resultToDisplay[i].span+"</b><br><div class='fakelink' style='padding: 5px 0 5px 0px;' onclick='gbrowseThumbnail(\""+resultToDisplay[i].span+"\",\"fly\",\""+resultToDisplay[i].gbrowseurl+"\")'>GBrowse thumbnail</div><div style='align:center'><a title='View GBrowse tracks on modENCODE GBrowse server' target='_blank' href='${GBROWSE_BASE_URL}fly/?"+resultToDisplay[i].gbrowseurl+"'>GBrowse tracks<img border='0' title='fly' src='model/images/fly_gb.png' class='arrow'></a></div><div style='align:center; padding-bottom:12px'><span class='fakelink exportDiv'> Export data </span><img class='exportDiv' style='position:relative; top:3px;' border='0' src='model/images/download.png' title='export data' height='18' width='18'/><ul class='contextMenu'><li class='tab'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"tab\");' class='ext_link'>TAB</a></li><li class='csv'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"csv\");' class='ext_link'>CSV</a></li><li class='gff'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"gff3\");' class='ext_link'>GFF3</a></li><li class='seq'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"sequence\");' class='ext_link'>SEQ</a></li></ul></div><div style='align:center'><a href='javascript: exportToGalaxy(\""+resultToDisplay[i].span+"\");' class='ext_link'> Export to Galaxy <img border='0' title='Export to Galaxy' src='model/images/Galaxy_logo_small.png' class='arrow' style='height:13%; width:13%'></a></div></td><td><a target='_blank' title='" + features[0].featurePId + "' href='" + "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}" + "/portal.do?externalid=" + features[0].featurePId + "&class=" + features[0].featureType + "'>" + features[0].featurePId.substring(0,50) + "...</a></td><td>"+features[0].featureType+"</td><td>"+features[0].location+"</td><td>"+features[0].machedBaseCount+"</td><td><a target='_blank' href='" + "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}" + "/portal.do?externalid=" + features[0].dccId + "&class=Submission'>" + features[0].dccId + "</a></td><td>" + features[0].subTitle + "</td></tr>");
                        }

                        if (features[0].featurePId.length <= 50 && features[0].subTitle.length > 50) {
                            jQuery("#spanResultsTable > tbody").append("<tr><td valign='top' rowspan='"+features.length+"'><b>"+resultToDisplay[i].span+"</b><br><div class='fakelink' style='padding: 5px 0 5px 0px;' onclick='gbrowseThumbnail(\""+resultToDisplay[i].span+"\",\"fly\",\""+resultToDisplay[i].gbrowseurl+")\"'>GBrowse thumbnail</div><div style='align:center'><a title='View GBrowse tracks on modENCODE GBrowse server' target='_blank' href='${GBROWSE_BASE_URL}fly/?"+resultToDisplay[i].gbrowseurl+"'>GBrowse tracks<img border='0' title='fly' src='model/images/fly_gb.png' class='arrow'></a></div><div style='align:center; padding-bottom:12px'><span class='fakelink exportDiv'> Export data </span><img class='exportDiv' style='position:relative; top:3px;' border='0' src='model/images/download.png' title='export data' height='18' width='18'/><ul class='contextMenu'><li class='tab'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"tab\");' class='ext_link'>TAB</a></li><li class='csv'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"csv\");' class='ext_link'>CSV</a></li><li class='gff'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"gff3\");' class='ext_link'>GFF3</a></li><li class='seq'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"sequence\");' class='ext_link'>SEQ</a></li></ul></div><div style='align:center'><a href='javascript: exportToGalaxy(\""+resultToDisplay[i].span+"\");' class='ext_link'> Export to Galaxy <img border='0' title='Export to Galaxy' src='model/images/Galaxy_logo_small.png' class='arrow' style='height:13%; width:13%'></a></div></td><td><a target='_blank' title='" + features[0].featurePId + "' href='" + "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}" + "/portal.do?externalid=" + features[0].featurePId + "&class=" + features[0].featureType + "'>" + features[0].featurePId + "</a></td><td>"+features[0].featureType+"</td><td>"+features[0].location+"</td><td>"+features[0].machedBaseCount+"</td><td><a target='_blank' href='" + "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}" + "/portal.do?externalid=" + features[0].dccId + "&class=Submission'>" + features[0].dccId + "</a></td><td><span title='" + features[0].subTitle + "'>" + features[0].subTitle.substring(0,50) + "...</span></td></tr>");
                        }

                        if (features[0].featurePId.length <= 50 && features[0].subTitle.length <= 50) {
                            jQuery("#spanResultsTable > tbody").append("<tr><td valign='top' rowspan='"+features.length+"'><b>"+resultToDisplay[i].span+"</b><br><div class='fakelink' style='padding: 5px 0 5px 0px;' onclick='gbrowseThumbnail(\""+resultToDisplay[i].span+"\",\"fly\",\""+resultToDisplay[i].gbrowseurl+"\")'>GBrowse thumbnail</div><div style='align:center'><a title='View GBrowse tracks on modENCODE GBrowse server' target='_blank' href='${GBROWSE_BASE_URL}fly/?"+resultToDisplay[i].gbrowseurl+"'>GBrowse tracks<img border='0' title='fly' src='model/images/fly_gb.png' class='arrow'></a></div><div style='align:center; padding-bottom:12px'><span class='fakelink exportDiv'> Export data </span><img class='exportDiv' style='position:relative; top:3px;' border='0' src='model/images/download.png' title='export data' height='18' width='18'/><ul class='contextMenu'><li class='tab'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"tab\");' class='ext_link'>TAB</a></li><li class='csv'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"csv\");' class='ext_link'>CSV</a></li><li class='gff'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"gff3\");' class='ext_link'>GFF3</a></li><li class='seq'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"sequence\");' class='ext_link'>SEQ</a></li></ul></div><div style='align:center'><a href='javascript: exportToGalaxy(\""+resultToDisplay[i].span+"\");' class='ext_link'> Export to Galaxy <img border='0' title='Export to Galaxy' src='model/images/Galaxy_logo_small.png' class='arrow' style='height:13%; width:13%'></a></div></td><td><a target='_blank' title='" + features[0].featurePId + "' href='" + "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}" + "/portal.do?externalid=" + features[0].featurePId + "&class=" + features[0].featureType + "'>" + features[0].featurePId + "</a></td><td>"+features[0].featureType+"</td><td>"+features[0].location+"</td><td>"+features[0].machedBaseCount+"</td><td><a target='_blank' href='" + "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}" + "/portal.do?externalid=" + features[0].dccId + "&class=Submission'>" + features[0].dccId + "</a></td><td>" + features[0].subTitle + "</td></tr>");
                        }
                    }
                    else if (resultToDisplay[i].organism == "C. elegans") {
                        if (features[0].featurePId.length > 50 && features[0].subTitle.length > 50) {
                            jQuery("#spanResultsTable > tbody").append("<tr><td valign='top' rowspan='"+features.length+"'><b>"+resultToDisplay[i].span+"</b><br><div class='fakelink' style='padding: 5px 0 5px 0px;' onclick='gbrowseThumbnail(\""+resultToDisplay[i].span+"\",\"worm\",\""+resultToDisplay[i].gbrowseurl+"\")'>GBrowse thumbnail</div><div style='align:center'><a title='View GBrowse tracks on modENCODE GBrowse server' target='_blank' href='${GBROWSE_BASE_URL}worm/?"+resultToDisplay[i].gbrowseurl+"'>GBrowse tracks<img border='0' title='worm' src='model/images/worm_gb.png' class='arrow'></a></div><div style='align:center; padding-bottom:12px'><span class='fakelink exportDiv'> Export data </span><img class='exportDiv' style='position:relative; top:3px;' border='0' src='model/images/download.png' title='export data' height='18' width='18'/><ul class='contextMenu'><li class='tab'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"tab\");' class='ext_link'>TAB</a></li><li class='csv'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"csv\");' class='ext_link'>CSV</a></li><li class='gff'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"gff3\");' class='ext_link'>GFF3</a></li><li class='seq'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"sequence\");' class='ext_link'>SEQ</a></li></ul></div><div style='align:center'><a href='javascript: exportToGalaxy(\""+resultToDisplay[i].span+"\");' class='ext_link'> Export to Galaxy <img border='0' title='Export to Galaxy' src='model/images/Galaxy_logo_small.png' class='arrow' style='height:13%; width:13%'></a></div></td><td><a target='_blank' title='" + features[0].featurePId + "' href='" + "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}" + "/portal.do?externalid=" + features[0].featurePId + "&class=" + features[0].featureType + "'>" + features[0].featurePId.substring(0,50) + "...</a></td><td>"+features[0].featureType+"</td><td>"+features[0].location+"</td><td>"+features[0].machedBaseCount+"</td><td><a target='_blank' href='" + "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}" + "/portal.do?externalid=" + features[0].dccId + "&class=Submission'>" + features[0].dccId + "</a></td><td><span title='" + features[0].subTitle + "'>" + features[0].subTitle.substring(0,50) + "...</span></td></tr>");
                        }

                        if (features[0].featurePId.length > 50 && features[0].subTitle.length <= 50) {
                            jQuery("#spanResultsTable > tbody").append("<tr><td valign='top' rowspan='"+features.length+"'><b>"+resultToDisplay[i].span+"</b><br><div class='fakelink' style='padding: 5px 0 5px 0px;' onclick='gbrowseThumbnail(\""+resultToDisplay[i].span+"\",\"worm\",\""+resultToDisplay[i].gbrowseurl+"\")'>GBrowse thumbnail</div><div style='align:center'><a title='View GBrowse tracks on modENCODE GBrowse server' target='_blank' href='${GBROWSE_BASE_URL}worm/?"+resultToDisplay[i].gbrowseurl+"'>GBrowse tracks<img border='0' title='worm' src='model/images/worm_gb.png' class='arrow'></a></div><div style='align:center; padding-bottom:12px'><span class='fakelink exportDiv'> Export data </span><img class='exportDiv' style='position:relative; top:3px;' border='0' src='model/images/download.png' title='export data' height='18' width='18'/><ul class='contextMenu'><li class='tab'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"tab\");' class='ext_link'>TAB</a></li><li class='csv'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"csv\");' class='ext_link'>CSV</a></li><li class='gff'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"gff3\");' class='ext_link'>GFF3</a></li><li class='seq'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"sequence\");' class='ext_link'>SEQ</a></li></ul></div><div style='align:center'><a href='javascript: exportToGalaxy(\""+resultToDisplay[i].span+"\");' class='ext_link'> Export to Galaxy <img border='0' title='Export to Galaxy' src='model/images/Galaxy_logo_small.png' class='arrow' style='height:13%; width:13%'></a></div></td><td><a target='_blank' title='" + features[0].featurePId + "' href='" + "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}" + "/portal.do?externalid=" + features[0].featurePId + "&class=" + features[0].featureType + "'>" + features[0].featurePId.substring(0,50) + "...</a></td><td>"+features[0].featureType+"</td><td>"+features[0].location+"</td><td>"+features[0].machedBaseCount+"</td><td><a target='_blank' href='" + "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}" + "/portal.do?externalid=" + features[0].dccId + "&class=Submission'>" + features[0].dccId + "</a></td><td>" + features[0].subTitle + "</td></tr>");
                        }

                        if (features[0].featurePId.length <= 50 && features[0].subTitle.length > 50) {
                            jQuery("#spanResultsTable > tbody").append("<tr><td valign='top' rowspan='"+features.length+"'><b>"+resultToDisplay[i].span+"</b><br><div class='fakelink' style='padding: 5px 0 5px 0px;' onclick='gbrowseThumbnail(\""+resultToDisplay[i].span+"\",\"worm\",\""+resultToDisplay[i].gbrowseurl+"\")'>GBrowse thumbnail</div><div style='align:center'><a title='View GBrowse tracks on modENCODE GBrowse server' target='_blank' href='${GBROWSE_BASE_URL}worm/?"+resultToDisplay[i].gbrowseurl+"'>GBrowse tracks<img border='0' title='worm' src='model/images/worm_gb.png' class='arrow'></a></div><div style='align:center; padding-bottom:12px'><span class='fakelink exportDiv'> Export data </span><img class='exportDiv' style='position:relative; top:3px;' border='0' src='model/images/download.png' title='export data' height='18' width='18'/><ul class='contextMenu'><li class='tab'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"tab\");' class='ext_link'>TAB</a></li><li class='csv'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"csv\");' class='ext_link'>CSV</a></li><li class='gff'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"gff3\");' class='ext_link'>GFF3</a></li><li class='seq'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"sequence\");' class='ext_link'>SEQ</a></li></ul></div><div style='align:center'><a href='javascript: exportToGalaxy(\""+resultToDisplay[i].span+"\");' class='ext_link'> Export to Galaxy <img border='0' title='Export to Galaxy' src='model/images/Galaxy_logo_small.png' class='arrow' style='height:13%; width:13%'></a></div></td><td><a target='_blank' title='" + features[0].featurePId + "' href='" + "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}" + "/portal.do?externalid=" + features[0].featurePId + "&class=" + features[0].featureType + "'>" + features[0].featurePId + "</a></td><td>"+features[0].featureType+"</td><td>"+features[0].location+"</td><td>"+features[0].machedBaseCount+"</td><td><a target='_blank' href='" + "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}" + "/portal.do?externalid=" + features[0].dccId + "&class=Submission'>" + features[0].dccId + "</a></td><td><span title='" + features[0].subTitle + "'>" + features[0].subTitle.substring(0,50) + "...</span></td></tr>");
                        }

                        if (features[0].featurePId.length <= 50 && features[0].subTitle.length <= 50) {
                            jQuery("#spanResultsTable > tbody").append("<tr><td valign='top' rowspan='"+features.length+"'><b>"+resultToDisplay[i].span+"</b><br><div class='fakelink' style='padding: 5px 0 5px 0px;' onclick='gbrowseThumbnail(\""+resultToDisplay[i].span+"\",\"worm\",\""+resultToDisplay[i].gbrowseurl+"\")'>GBrowse thumbnail</div><div style='align:center'><a title='View GBrowse tracks on modENCODE GBrowse server' target='_blank' href='${GBROWSE_BASE_URL}worm/?"+resultToDisplay[i].gbrowseurl+"'>GBrowse tracks<img border='0' title='worm' src='model/images/worm_gb.png' class='arrow'></a></div><div style='align:center; padding-bottom:12px'><span class='fakelink exportDiv'> Export data </span><img class='exportDiv' style='position:relative; top:3px;' border='0' src='model/images/download.png' title='export data' height='18' width='18'/><ul class='contextMenu'><li class='tab'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"tab\");' class='ext_link'>TAB</a></li><li class='csv'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"csv\");' class='ext_link'>CSV</a></li><li class='gff'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"gff3\");' class='ext_link'>GFF3</a></li><li class='seq'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"sequence\");' class='ext_link'>SEQ</a></li></ul></div><div style='align:center'><a href='javascript: exportToGalaxy(\""+resultToDisplay[i].span+"\");' class='ext_link'> Export to Galaxy <img border='0' title='Export to Galaxy' src='model/images/Galaxy_logo_small.png' class='arrow' style='height:13%; width:13%'></a></div></td><td><a target='_blank' title='" + features[0].featurePId + "' href='" + "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}" + "/portal.do?externalid=" + features[0].featurePId + "&class=" + features[0].featureType + "'>" + features[0].featurePId + "</a></td><td>"+features[0].featureType+"</td><td>"+features[0].location+"</td><td>"+features[0].machedBaseCount+"</td><td><a target='_blank' href='" + "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}" + "/portal.do?externalid=" + features[0].dccId + "&class=Submission'>" + features[0].dccId + "</a></td><td>" + features[0].subTitle + "</td></tr>");
                        }
                    }
                }
                else {
                    if(resultToDisplay[i].organism == "D. melanogaster") {
                        if (features[0].featurePId.length > 50 && features[0].subTitle.length > 50) {
                            jQuery("#spanResultsTable > tbody").append("<tr><td valign='top' rowspan='"+features.length+"'><b>"+resultToDisplay[i].span+"</b><br><div style='align:center'><span style='color:grey;' title='No tracks available'>GBrowse tracks</span><img border='0' title='fly' src='model/images/fly_gb.png' class='arrow'></div><div style='align:center; padding-bottom:12px'><span class='fakelink exportDiv'> Export data </span><img class='exportDiv' style='position:relative; top:3px;' border='0' src='model/images/download.png' title='export data' height='18' width='18'/><ul class='contextMenu'><li class='tab'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"tab\");' class='ext_link'>TAB</a></li><li class='csv'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"csv\");' class='ext_link'>CSV</a></li><li class='gff'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"gff3\");' class='ext_link'>GFF3</a></li><li class='seq'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"sequence\");' class='ext_link'>SEQ</a></li></ul></div><div style='align:center'><a href='javascript: exportToGalaxy(\""+resultToDisplay[i].span+"\");' class='ext_link'> Export to Galaxy <img border='0' title='Export to Galaxy' src='model/images/Galaxy_logo_small.png' class='arrow' style='height:13%; width:13%'></a></div></td><td><a target='_blank' title='" + features[0].featurePId + "' href='" + "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}" + "/portal.do?externalid=" + features[0].featurePId + "&class=" + features[0].featureType + "'>" + features[0].featurePId.substring(0,50) + "...</a></td><td>"+features[0].featureType+"</td><td>"+features[0].location+"</td><td>"+features[0].machedBaseCount+"</td><td><a target='_blank' href='" + "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}" + "/portal.do?externalid=" + features[0].dccId + "&class=Submission'>" + features[0].dccId + "</a></td><td><span title='" + features[0].subTitle + "'>" + features[0].subTitle.substring(0,50) + "...</span></td></tr>");
                        }

                        if (features[0].featurePId.length > 50 && features[0].subTitle.length <= 50) {
                            jQuery("#spanResultsTable > tbody").append("<tr><td valign='top' rowspan='"+features.length+"'><b>"+resultToDisplay[i].span+"</b><br><div style='align:center'><span style='color:grey;' title='No tracks available'>GBrowse tracks</span><img border='0' title='fly' src='model/images/fly_gb.png' class='arrow'></div><div style='align:center; padding-bottom:12px'><span class='fakelink exportDiv'> Export data </span><img class='exportDiv' style='position:relative; top:3px;' border='0' src='model/images/download.png' title='export data' height='18' width='18'/><ul class='contextMenu'><li class='tab'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"tab\");' class='ext_link'>TAB</a></li><li class='csv'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"csv\");' class='ext_link'>CSV</a></li><li class='gff'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"gff3\");' class='ext_link'>GFF3</a></li><li class='seq'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"sequence\");' class='ext_link'>SEQ</a></li></ul></div><div style='align:center'><a href='javascript: exportToGalaxy(\""+resultToDisplay[i].span+"\");' class='ext_link'> Export to Galaxy <img border='0' title='Export to Galaxy' src='model/images/Galaxy_logo_small.png' class='arrow' style='height:13%; width:13%'></a></div></td><td><a target='_blank' title='" + features[0].featurePId + "' href='" + "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}" + "/portal.do?externalid=" + features[0].featurePId + "&class=" + features[0].featureType + "'>" + features[0].featurePId.substring(0,50) + "...</a></td><td>"+features[0].featureType+"</td><td>"+features[0].location+"</td><td>"+features[0].machedBaseCount+"</td><td><a target='_blank' href='" + "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}" + "/portal.do?externalid=" + features[0].dccId + "&class=Submission'>" + features[0].dccId + "</a></td><td>" + features[0].subTitle + "</td></tr>");
                        }

                        if (features[0].featurePId.length <= 50 && features[0].subTitle.length > 50) {
                            jQuery("#spanResultsTable > tbody").append("<tr><td valign='top' rowspan='"+features.length+"'><b>"+resultToDisplay[i].span+"</b><br><div style='align:center'><span style='color:grey;' title='No tracks available'>GBrowse tracks</span><img border='0' title='fly' src='model/images/fly_gb.png' class='arrow'></div><div style='align:center; padding-bottom:12px'><span class='fakelink exportDiv'> Export data </span><img class='exportDiv' style='position:relative; top:3px;' border='0' src='model/images/download.png' title='export data' height='18' width='18'/><ul class='contextMenu'><li class='tab'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"tab\");' class='ext_link'>TAB</a></li><li class='csv'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"csv\");' class='ext_link'>CSV</a></li><li class='gff'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"gff3\");' class='ext_link'>GFF3</a></li><li class='seq'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"sequence\");' class='ext_link'>SEQ</a></li></ul></div><div style='align:center'><a href='javascript: exportToGalaxy(\""+resultToDisplay[i].span+"\");' class='ext_link'> Export to Galaxy <img border='0' title='Export to Galaxy' src='model/images/Galaxy_logo_small.png' class='arrow' style='height:13%; width:13%'></a></div></td><td><a target='_blank' title='" + features[0].featurePId + "' href='" + "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}" + "/portal.do?externalid=" + features[0].featurePId + "&class=" + features[0].featureType + "'>" + features[0].featurePId + "</a></td><td>"+features[0].featureType+"</td><td>"+features[0].location+"</td><td>"+features[0].machedBaseCount+"</td><td><a target='_blank' href='" + "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}" + "/portal.do?externalid=" + features[0].dccId + "&class=Submission'>" + features[0].dccId + "</a></td><td><span title='" + features[0].subTitle + "'>" + features[0].subTitle.substring(0,50) + "...</span></td></tr>");
                        }

                        if (features[0].featurePId.length <= 50 && features[0].subTitle.length <= 50) {
                            jQuery("#spanResultsTable > tbody").append("<tr><td valign='top' rowspan='"+features.length+"'><b>"+resultToDisplay[i].span+"</b><br><div style='align:center'><span style='color:grey;' title='No tracks available'>GBrowse tracks</span><img border='0' title='fly' src='model/images/fly_gb.png' class='arrow'></div><div style='align:center; padding-bottom:12px'><span class='fakelink exportDiv'> Export data </span><img class='exportDiv' style='position:relative; top:3px;' border='0' src='model/images/download.png' title='export data' height='18' width='18'/><ul class='contextMenu'><li class='tab'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"tab\");' class='ext_link'>TAB</a></li><li class='csv'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"csv\");' class='ext_link'>CSV</a></li><li class='gff'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"gff3\");' class='ext_link'>GFF3</a></li><li class='seq'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"sequence\");' class='ext_link'>SEQ</a></li></ul></div><div style='align:center'><a href='javascript: exportToGalaxy(\""+resultToDisplay[i].span+"\");' class='ext_link'> Export to Galaxy <img border='0' title='Export to Galaxy' src='model/images/Galaxy_logo_small.png' class='arrow' style='height:13%; width:13%'></a></div></td><td><a target='_blank' title='" + features[0].featurePId + "' href='" + "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}" + "/portal.do?externalid=" + features[0].featurePId + "&class=" + features[0].featureType + "'>" + features[0].featurePId + "</a></td><td>"+features[0].featureType+"</td><td>"+features[0].location+"</td><td>"+features[0].machedBaseCount+"</td><td><a target='_blank' href='" + "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}" + "/portal.do?externalid=" + features[0].dccId + "&class=Submission'>" + features[0].dccId + "</a></td><td>" + features[0].subTitle + "</td></tr>");
                        }
                    }
                    else if (resultToDisplay[i].organism == "C. elegans") {
                        if (features[0].featurePId.length > 50 && features[0].subTitle.length > 50) {
                            jQuery("#spanResultsTable > tbody").append("<tr><td valign='top' rowspan='"+features.length+"'><b>"+resultToDisplay[i].span+"</b><br><div style='align:center'><span style='color:grey;' title='No tracks available'>GBrowse tracks</span><img border='0' title='worm' src='model/images/worm_gb.png' class='arrow'></div><div style='align:center; padding-bottom:12px'><span class='fakelink exportDiv'> Export data </span><img class='exportDiv' style='position:relative; top:3px;' border='0' src='model/images/download.png' title='export data' height='18' width='18'/><ul class='contextMenu'><li class='tab'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"tab\");' class='ext_link'>TAB</a></li><li class='csv'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"csv\");' class='ext_link'>CSV</a></li><li class='gff'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"gff3\");' class='ext_link'>GFF3</a></li><li class='seq'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"sequence\");' class='ext_link'>SEQ</a></li></ul></div><div style='align:center'><a href='javascript: exportToGalaxy(\""+resultToDisplay[i].span+"\");' class='ext_link'> Export to Galaxy <img border='0' title='Export to Galaxy' src='model/images/Galaxy_logo_small.png' class='arrow' style='height:13%; width:13%'></a></div></td><td><a target='_blank' title='" + features[0].featurePId + "' href='" + "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}" + "/portal.do?externalid=" + features[0].featurePId + "&class=" + features[0].featureType + "'>" + features[0].featurePId.substring(0,50) + "...</a></td><td>"+features[0].featureType+"</td><td>"+features[0].location+"</td><td>"+features[0].machedBaseCount+"</td><td><a target='_blank' href='" + "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}" + "/portal.do?externalid=" + features[0].dccId + "&class=Submission'>" + features[0].dccId + "</a></td><td><span title='" + features[0].subTitle + "'>" + features[0].subTitle.substring(0,50) + "...</span></td></tr>");
                        }

                        if (features[0].featurePId.length > 50 && features[0].subTitle.length <= 50) {
                            jQuery("#spanResultsTable > tbody").append("<tr><td valign='top' rowspan='"+features.length+"'><b>"+resultToDisplay[i].span+"</b><br><div style='align:center'><span style='color:grey;' title='No tracks available'>GBrowse tracks</span><img border='0' title='worm' src='model/images/worm_gb.png' class='arrow'></div><div style='align:center; padding-bottom:12px'><span class='fakelink exportDiv'> Export data </span><img class='exportDiv' style='position:relative; top:3px;' border='0' src='model/images/download.png' title='export data' height='18' width='18'/><ul class='contextMenu'><li class='tab'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"tab\");' class='ext_link'>TAB</a></li><li class='csv'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"csv\");' class='ext_link'>CSV</a></li><li class='gff'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"gff3\");' class='ext_link'>GFF3</a></li><li class='seq'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"sequence\");' class='ext_link'>SEQ</a></li></ul></div><div style='align:center'><a href='javascript: exportToGalaxy(\""+resultToDisplay[i].span+"\");' class='ext_link'> Export to Galaxy <img border='0' title='Export to Galaxy' src='model/images/Galaxy_logo_small.png' class='arrow' style='height:13%; width:13%'></a></div></td><td><a target='_blank' title='" + features[0].featurePId + "' href='" + "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}" + "/portal.do?externalid=" + features[0].featurePId + "&class=" + features[0].featureType + "'>" + features[0].featurePId.substring(0,50) + "...</a></td><td>"+features[0].featureType+"</td><td>"+features[0].location+"</td><td>"+features[0].machedBaseCount+"</td><td><a target='_blank' href='" + "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}" + "/portal.do?externalid=" + features[0].dccId + "&class=Submission'>" + features[0].dccId + "</a></td><td>" + features[0].subTitle + "</td></tr>");
                        }

                        if (features[0].featurePId.length <= 50 && features[0].subTitle.length > 50) {
                            jQuery("#spanResultsTable > tbody").append("<tr><td valign='top' rowspan='"+features.length+"'><b>"+resultToDisplay[i].span+"</b><br><div style='align:center'><span style='color:grey;' title='No tracks available'>GBrowse tracks</span><img border='0' title='worm' src='model/images/worm_gb.png' class='arrow'></div><div style='align:center; padding-bottom:12px'><span class='fakelink exportDiv'> Export data </span><img class='exportDiv' style='position:relative; top:3px;' border='0' src='model/images/download.png' title='export data' height='18' width='18'/><ul class='contextMenu'><li class='tab'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"tab\");' class='ext_link'>TAB</a></li><li class='csv'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"csv\");' class='ext_link'>CSV</a></li><li class='gff'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"gff3\");' class='ext_link'>GFF3</a></li><li class='seq'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"sequence\");' class='ext_link'>SEQ</a></li></ul></div><div style='align:center'><a href='javascript: exportToGalaxy(\""+resultToDisplay[i].span+"\");' class='ext_link'> Export to Galaxy <img border='0' title='Export to Galaxy' src='model/images/Galaxy_logo_small.png' class='arrow' style='height:13%; width:13%'></a></div></td><td><a target='_blank' title='" + features[0].featurePId + "' href='" + "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}" + "/portal.do?externalid=" + features[0].featurePId + "&class=" + features[0].featureType + "'>" + features[0].featurePId + "</a></td><td>"+features[0].featureType+"</td><td>"+features[0].location+"</td><td>"+features[0].machedBaseCount+"</td><td><a target='_blank' href='" + "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}" + "/portal.do?externalid=" + features[0].dccId + "&class=Submission'>" + features[0].dccId + "</a></td><td><span title='" + features[0].subTitle + "'>" + features[0].subTitle.substring(0,50) + "...</span></td></tr>");
                        }

                        if (features[0].featurePId.length <= 50 && features[0].subTitle.length <= 50) {
                            jQuery("#spanResultsTable > tbody").append("<tr><td valign='top' rowspan='"+features.length+"'><b>"+resultToDisplay[i].span+"</b><br><div style='align:center'><span style='color:grey;' title='No tracks available'>GBrowse tracks</span><img border='0' title='worm' src='model/images/worm_gb.png' class='arrow'></div><div style='align:center; padding-bottom:12px'><span class='fakelink exportDiv'> Export data </span><img class='exportDiv' style='position:relative; top:3px;' border='0' src='model/images/download.png' title='export data' height='18' width='18'/><ul class='contextMenu'><li class='tab'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"tab\");' class='ext_link'>TAB</a></li><li class='csv'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"csv\");' class='ext_link'>CSV</a></li><li class='gff'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"gff3\");' class='ext_link'>GFF3</a></li><li class='seq'><a href='#javascript: exportFeatures(\""+resultToDisplay[i].span+"\", \"sequence\");' class='ext_link'>SEQ</a></li></ul></div><div style='align:center'><a href='javascript: exportToGalaxy(\""+resultToDisplay[i].span+"\");' class='ext_link'> Export to Galaxy <img border='0' title='Export to Galaxy' src='model/images/Galaxy_logo_small.png' class='arrow' style='height:13%; width:13%'></a></div></td><td><a target='_blank' title='" + features[0].featurePId + "' href='" + "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}" + "/portal.do?externalid=" + features[0].featurePId + "&class=" + features[0].featureType + "'>" + features[0].featurePId + "</a></td><td>"+features[0].featureType+"</td><td>"+features[0].location+"</td><td>"+features[0].machedBaseCount+"</td><td><a target='_blank' href='" + "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}" + "/portal.do?externalid=" + features[0].dccId + "&class=Submission'>" + features[0].dccId + "</a></td><td>" + features[0].subTitle + "</td></tr>");
                        }
                    }
                }

                for (j=1; j<features.length; j++) {
                    if (features[j].featurePId.length > 50 && features[j].subTitle.length > 50) {
                        jQuery("#spanResultsTable > tbody").append("<tr><td><a target='_blank' title='" + features[j].featurePId + "' href='" + "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}" + "/portal.do?externalid=" + features[j].featurePId + "&class=" + features[j].featureType + "'>" + features[j].featurePId.substring(0,50) + "...</a></td><td>"+features[j].featureType+"</td><td>"+features[j].location+"</td><td>"+features[j].machedBaseCount+"</td><td><a target='_blank' href='" + "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}" + "/portal.do?externalid=" + features[j].dccId + "&class=Submission'>" + features[j].dccId + "</a></td><td><span title='" + features[j].subTitle + "'>" + features[j].subTitle.substring(0,50) + "...</span></td></tr>");
                    }

                    if (features[j].featurePId.length > 50 && features[j].subTitle.length <= 50) {
                        jQuery("#spanResultsTable > tbody").append("<tr><td><a target='_blank' title='" + features[j].featurePId + "' href='" + "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}" + "/portal.do?externalid=" + features[j].featurePId + "&class=" + features[j].featureType + "'>" + features[j].featurePId.substring(0,50) + "...</a></td><td>"+features[j].featureType+"</td><td>"+features[j].location+"</td><td>"+features[j].machedBaseCount+"</td><td><a target='_blank' href='" + "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}" + "/portal.do?externalid=" + features[j].dccId + "&class=Submission'>" + features[j].dccId + "</a></td><td>" + features[j].subTitle + "</td></tr>");
                    }

                    if (features[j].featurePId.length <= 50 && features[j].subTitle.length > 50) {
                        jQuery("#spanResultsTable > tbody").append("<tr><td><a target='_blank' title='" + features[j].featurePId + "' href='" + "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}" + "/portal.do?externalid=" + features[j].featurePId + "&class=" + features[j].featureType + "'>" + features[j].featurePId + "</a></td><td>"+features[j].featureType+"</td><td>"+features[j].location+"</td><td>"+features[j].machedBaseCount+"</td><td><a target='_blank' href='" + "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}" + "/portal.do?externalid=" + features[j].dccId + "&class=Submission'>" + features[j].dccId + "</a></td><td><span title='" + features[j].subTitle + "'>" + features[j].subTitle.substring(0,50) + "...</span></td></tr>");
                    }

                    if (features[j].featurePId.length <= 50 && features[j].subTitle.length <= 50) {
                        jQuery("#spanResultsTable > tbody").append("<tr><td><a target='_blank' title='" + features[j].featurePId + "' href='" + "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}" + "/portal.do?externalid=" + features[j].featurePId + "&class=" + features[j].featureType + "'>" + features[j].featurePId + "</a></td><td>"+features[j].featureType+"</td><td>"+features[j].location+"</td><td>"+features[j].machedBaseCount+"</td><td><a target='_blank' href='" + "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}" + "/portal.do?externalid=" + features[j].dccId + "&class=Submission'>" + features[j].dccId + "</a></td><td>" + features[j].subTitle + "</td></tr>");
                    }
                }
            }
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

    function updatePageNavBarAfterQueryFinish() {
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
            updatePageNavBarAfterQueryFinish();
        }
    }

    function getTotalPageNumber() {
        total_page = Math.floor(spanQueryTotalCount / current_page_size);
        if (spanQueryTotalCount % current_page_size != 0) { total_page++; }

        return total_page;
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

    <%-- Pagination --%>
    <div style="float: right; margin-right: 35px;">Page size
    <select id="pageSizeList" onchange="changePageSize()" name="pageSize">
        <option value="10">10</option>
        <option value="25">25</option>
        <option value="50">50</option>
        <option value="100">100</option>
    </select>

    <span id="upperNav">
    </span>

    </div>
    <%-- /Pagination --%>

    <%-- Export All links, refer to attributeLinkDisplayer.jsp --%>
    <div id="exportAll" style="display: inline; padding: 0px 0px 0px 23px; line-height: 22px;">
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
    </tbody>
  </table>
</div>

<div id="bottomDiv" align="center">
    <%-- Pagination at bottom --%>
    <span id="lowerNav"></span>
    <%-- /Pagination at bottom --%>
    </div>

</div>

<div id="gbrowseThumbnail"></div>

<!--  /spanUploadResults.jsp -->
