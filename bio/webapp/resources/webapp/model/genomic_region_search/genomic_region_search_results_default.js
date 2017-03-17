
    var is_all_queries_finished = false;
    var finishedQueryCount = 0;
    var current_page_size = 10;
    var current_page_no = 1;
    var spanResultWaitingIntervalId = "";

    jQuery(document).ready(function(){
       init();

       // polling
       jQuery.PeriodicalUpdater("genomicRegionSearchAjax.do", {
           method: 'post',         // method; get or post
           data: {spanUUIDString: span_uuid_string, getProgress: "true"}, // array of values to be passed to the page - e.g. {name: "John", greeting: "hello"}
           minTimeout: 500,          // starting value for the timeout in milliseconds
           maxTimeout: 5000,       // maximum length of time between requests
           multiplier: 2,          // if set to 2, timerInterval will double each time the response hasn't changed (up to maxTimeout)
           type: 'text',           // response type - text, xml, json, etc.  See $.ajax config options
           maxCalls: 0,            // maximum number of calls. 0 = no limit.
           autoStop: 50            // automatically stop requests after this many returns of the same data. 0 = disabled.
        }, function(data) {
           finishedQueryCount = parseInt(data);
           if (finishedQueryCount < span_query_total_count) {
               var percentage = Math.floor(100 * finishedQueryCount / span_query_total_count);
               jQuery("#progressbar").progressBar(percentage);
               jQuery("#progressbar_status").html(finishedQueryCount + "/" + span_query_total_count);
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

        jQuery.post("genomicRegionSearchAjax.do", { spanUUIDString: span_uuid_string, isEmptyFeature: "true" }, function(isEmptyFeature){
            if (isEmptyFeature.trim() == "hasFeature") {
                jQuery.post("genomicRegionSearchAjax.do", { spanUUIDString: span_uuid_string, generateCreateListHtml: "true" }, function(createListHtml){
                    if (export_chromosome_segment == "false") {
                        jQuery("#export-all-div").append('<span class="export-region">Export for all regions:</span>' +
                                '<span class="tab export-region"><a href="javascript: exportFeatures(\'all\', \'SequenceFeature\', \'tab\');"></a></span>' +
                                '<span class="csv export-region"><a href="javascript: exportFeatures(\'all\', \'SequenceFeature\', \'csv\');"></a></span>' +
                                '<span class="gff3 export-region"><a href="javascript: exportFeatures(\'all\', \'SequenceFeature\', \'gff3\');"></a></span>' +
                                '<span class="fasta export-region"><a href="javascript: exportFeatures(\'all\', \'SequenceFeature\', \'sequence\');"></a></span>' +
                                '<span class="bed export-region"><a href="javascript: exportFeatures(\'all\', \'SequenceFeature\', \'bed\');"></a></span>' +
                                createListHtml);
                    } else {
                        jQuery("#export-all-div").append('<span class="export-region">Export for all regions:</span>' +
                                '<span class="tab export-region"><a href="javascript: exportFeatures(\'all\', \'SequenceFeature\', \'tab\');"></a></span>' +
                                '<span class="csv export-region"><a href="javascript: exportFeatures(\'all\', \'SequenceFeature\', \'csv\');"></a></span>' +
                                '<span class="gff3 export-region"><a href="javascript: exportFeatures(\'all\', \'SequenceFeature\', \'gff3\');"></a></span>' +
                                '<span class="fasta export-region"><a href="javascript: exportFeatures(\'all\', \'SequenceFeature\', \'sequence\');"></a></span>' +
                                '<span class="bed export-region"><a href="javascript: exportFeatures(\'all\', \'SequenceFeature\', \'bed\');"></a></span>' +
                                '<span class="export-region"><a href="javascript: exportFeatures(\'all\', \'\', \'chrSeg\');"><img title="export all chromosome regions as FASTA" class="fasta" style="margin-top: 0px;" src="model/images/fasta.gif"></a></span>' +
                                createListHtml);
                    }

                });
            } else {
                jQuery("#export-all-div").append('Export for all regions:&nbsp;<span style="color:grey;">TAB</span>&nbsp;|&nbsp;<span style="color:grey;">CSV</span>&nbsp;|&nbsp;<span style="color:grey;">GFF3</span>&nbsp;|&nbsp;<span style="color:grey;">SEQ</span> or Create List by feature type: <select></select>');
            }
         });
    }

    function exportFeatures(criteria, facet, format) {
        jQuery.download("genomicRegionSearchAjax.do", "exportFeatures=true&spanUUIDString=" + span_uuid_string + "&criteria=" + criteria + "&facet=" + facet + "&format=" + format);
    }

    function createList(criteria, id, facet) { // id e.g. I-100-200
        if (id) { // JS will convert null, undefined, 0 and "" to bollean false
          facet = jQuery("#"+id).val();
        }

        jQuery.post("genomicRegionSearchAjax.do", { spanUUIDString: span_uuid_string, getFeatureCount: "true", criteria: criteria, facet: facet }, function(count){
          var feature_count = parseInt(count);
          if (feature_count >= 100000) {
            alert("It is not allowed to create a list with 100,000+ genomic features...");
          } else {
              jQuery.post("genomicRegionSearchAjax.do", { spanUUIDString: span_uuid_string, createList: "true", criteria: criteria, facet: facet }, function(bagName){
                  window.location.href = "/" + webapp_path + "/bagDetails.do?bagName=" + bagName;
                  /*window.open(
                      "/" + webapp_path + "/bagDetails.do?bagName=" + bagName,
                      '_blank' // <- This is what makes it open in a new window.
                      );*/
              }, "text"); // would use but triggers pop up blocker
          }
        }, "text");
    }

    function exportToGalaxy(genomicRegion) {
        jQuery.post("genomicRegionSearchAjax.do", { spanUUIDString: span_uuid_string, getFeatures: "true", grString: genomicRegion }, function(featureIds){
            featureIds ='<input type="hidden" name="featureIds" value="' + featureIds + '" />';
            orgName ='<input type="hidden" name="orgName" value="' + genomicRegion.split("|")[genomicRegion.split("|").length - 1] + '" />';
            jQuery('<form action="galaxyExportOptions.do" method="post">' + featureIds + orgName + '</form>').appendTo('body').submit().remove();
        }, "text");
    }

    function changePageSize() {
      current_page_size = jQuery('#pageSizeList').val();
      current_page_no = 1;

      loadResultData(current_page_size, current_page_no);

      if(finishedQueryCount < span_query_total_count){
            updatePageNavBarBeforeQueryFinish();
        } else {
            updatePageNavBarAfterQueryFinish(current_page_no, current_page_size);
        }
    }

    function loadResultData(page_size, page_num) {

        clearInterval(spanResultWaitingIntervalId);

        var from_index = (page_num - 1) * page_size; // the start index in the result map
        var to_index = page_num * page_size -1; // the end index in the result map
        if (to_index > span_query_total_count)
            { to_index = span_query_total_count - 1;}

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

        jQuery.post("genomicRegionSearchAjax.do", { spanUUIDString: span_uuid_string, getData: "true", fromIdx: from_index, toIdx: to_index }, function(results){
            addResultToTable(results);
        }, "html");
    }

    function addResultToTable(spanResult) {
        jQuery("#genomic-region-results-table").empty();
        resultToDisplay = spanResult.paginatedSpanResult;
        jQuery("#genomic-region-results-table").append(spanResult);
    }

    function gbrowseThumbnail(title, organism, url) {

        gb_img_url = gbrowse_image_url + organism + "/?" + url+";width=600;b=1";
        gb_server_url = gbrowse_base_url + organism + "/?" + url;
        jQuery("#gbrowseThumbnail").html("<a href='" + gb_server_url + "' target='_blank'><img title='GBrowse' src='" + gb_img_url + "'></a>");

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
                             .append('<span style="color:grey;">1 - '+span_query_total_count+' of '+span_query_total_count+'</span>')
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
                                   .append('<span style="color:grey;">1 - '+current_page_no*current_page_size+' of '+span_query_total_count+'</span>')
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
                                   .append('<span style="color:grey;">'+((current_page_no-1)*current_page_size+1)+' - '+span_query_total_count+' of '+span_query_total_count+'</span>')
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
                                   .append('<span style="color:grey;">'+((current_page_no-1)*current_page_size+1)+' - '+current_page_no*current_page_size+' of '+span_query_total_count+'</span>')
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
                             .append('<span style="color:grey;">1 - '+span_query_total_count+' of '+span_query_total_count+'</span>')
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
                                   .append('<span style="color:grey;">1 - '+current_page_no*current_page_size+' of '+span_query_total_count+'</span>')
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
                                   .append('<span style="color:grey;">'+((current_page_no-1)*current_page_size+1)+' - '+span_query_total_count+' of '+span_query_total_count+'</span>')
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
                                   .append('<span style="color:grey;">'+((current_page_no-1)*current_page_size+1)+' - '+current_page_no*current_page_size+' of '+span_query_total_count+'</span>')
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

        if(finishedQueryCount < span_query_total_count){
            updatePageNavBarBeforeQueryFinish();
        } else {
            updatePageNavBarAfterQueryFinish(current_page_no, current_page_size);
        }
    }

    function getTotalPageNumber() {
        total_page = Math.floor(span_query_total_count / current_page_size);
        if (span_query_total_count % current_page_size != 0) { total_page++; }

        return total_page;
    }

    function displayJBrowse() {
      jQuery.post("genomicRegionSearchAjax.do", { spanUUIDString: span_uuid_string, getDropDownList: "true" }, function(data){
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
                jQuery.post("genomicRegionSearchAjax.do", { spanUUIDString: span_uuid_string, getGivenRegionsResults: "true", regions: selectedRegion }, function(results){
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
