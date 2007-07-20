function setFavouriteTemplate(templateName, image){
    AjaxServices.setFavouriteTemplate(templateName);
    image.src='images/star_active.gif';
    image.onclick='';
    image.style.cursor='';
    image.title='This template is a favourite';
}

function precomputeTemplate(templateName){
    document.getElementById('precompute_'+templateName).innerHTML="Precomputing..";
    AjaxServices.preCompute(templateName,function(str) {
        document.getElementById('precompute_'+templateName).style.color="#777";
        document.getElementById('precompute_'+templateName).innerHTML="Precomputed";
     });
}

function summariseTemplate(templateName){
    document.getElementById('summarise_'+templateName).innerHTML="Summarising..";
    AjaxServices.summarise(templateName,function(str) {
        document.getElementById('summarise_'+templateName).style.color="#777";
        document.getElementById('summarise_'+templateName).innerHTML="Summarised";
    });
}

function editName(name){
  document.getElementById('form_'+name).style.display="block";
  document.getElementById('name_'+name).style.display="none";
}

function renameElement(name, type, index){
    document.getElementById('form_'+name).style.display="none";
    document.getElementById('name_'+name).innerHTML="<i>saving...</i>";
    document.getElementById('name_'+name).style.display="block";
    AjaxServices.rename(name,type, (document.getElementById('newName_'+name).value).replace(/^\s*|\s*$/g,""), function(str){
        document.getElementById('name_'+name).innerHTML=str;
        document.getElementById('selected_user_' + type + '_' + index).value=str;
    });
}

function changeViewPathDescription(pathName){
    var pathString = pathName.replace(/\_/g,".");
    var pathEnd = pathString.substring(pathString.lastIndexOf('.') + 1);
    document.getElementById('form_'+pathName).style.display = "none";
    document.getElementById('name_'+pathName+'_inner').innerHTML = "<i>saving...</i>";
    document.getElementById('name_'+pathName).style.display = "block";
    var newDescription = document.getElementById('newName_' + pathName).value;
    var callBack = function(prefixDescription){
        if (prefixDescription == null) {
            prefixDescription = '(no description)';
        }
        document.getElementById('name_' + pathName + '_inner').innerHTML =
          '<span  class="viewPathDescription">' + prefixDescription +
          '</span> &gt; ' + pathEnd;
    }
    AjaxServices.changeViewPathDescription(pathString, newDescription, callBack);
}

function saveBagDescription(bagName){
    var textarea = document.getElementById('textarea').value;
    textarea = textarea.replace(/[\n\r]+/g, "\n\r<br/>");
    document.getElementById('bagDescriptionDiv').innerHTML = '<i>Saving...</i>';
    AjaxServices.saveBagDescription(bagName,textarea, function(str){
        document.getElementById('bagDescriptionDiv').innerHTML = str;
    });
    swapDivs('bagDescriptionTextarea','bagDescriptionDiv');
}

function swapDivs(div1,div2){
    document.getElementById(div1).style.display = 'none';
    document.getElementById(div2).style.display = 'block';
}


function roundToSignificantDecimal(n) {
    var log10n = Math.log(Math.abs(n)) / Math.log(10);
    if(log10n<1 && n!=0) {
        var rf = 100*Math.pow(10,-Math.round(log10n));
        return rf;
    } else return 100;
}

function updateCountInColumnSummary() {
    var countString = document.resultsCountText;
    if (countString == null) {
        setTimeout("updateCountInColumnSummary()", 1000);
        return;
    }

    document.getElementById('summary_row_count').innerHTML = "<p>" + countString + "</p>";

    var est = document.getElementById('resultsCountEstimate');
    if (est == null || est.style.display != 'none') {
        setTimeout("updateCountInColumnSummary()", 500);
        return;
    }
}

function updateUniqueCountInColumnSummary(uniqueCountQid) {
    getResults(uniqueCountQid, 1000, resultsCountCallback, null);
}

function resultsCountCallback(results) {
    var size = results[0][0];

    if (size > 1) {
        var summaryUniqueCountElement = document.getElementById('summary_unique_count');
        summaryUniqueCountElement.style.display='inline';
        summaryUniqueCountElement.innerHTML='Total unique values: ' + size;
    }
    return true;
}

function getColumnSummary(tableName, columnName, columnDisplayName) {
    document.getElementById('summary_loaded').style.display = "none";
    document.getElementById('summary_loading').style.display = "block";
    Effect.Appear('summary', { duration: 0.30 });
    AjaxServices.getColumnSummary(tableName, columnName, function(str){
        var rows = str[0];
        function rounder(cell) {
            if (cell==null) {
                return "[no value]" ;
            } if (! isNaN(cell)) {
                var rf = roundToSignificantDecimal(cell+0);
                return Math.round(cell*rf)/rf;
            } else {
                return cell;
            }
        }
        var headerText;
        if(rows[0].length == 2){
            headerText = '<tr><th>Value</th><th>Count</th></tr>';
        } else {
            headerText = '<tr><th>Min</th><th>Max</th><th>Sample Mean</th><th>Standard Deviation</th></tr>';
        }

        document.getElementById('summary_loading').style.display = "none";
        document.getElementById('summary_loaded').style.display = "block";

        var bodyText = '';

        for (rowIndex = 0; rowIndex < rows.length; rowIndex++) {
            var row = rows[rowIndex];
            bodyText += '<tr>';
            for (colIndex = 0; colIndex < row.length; colIndex++) {
                var cellValue = rounder(row[colIndex]);
                bodyText += '<td>' + cellValue + '</td>';
            }
            bodyText += '</tr>';
        }

        var html = '<div class="box">                                         \
                      <div class="title">                                     \
                        Column Summary for ' + columnDisplayName + '          \
                      </div>                                                  \
                      <div id="summary_row_count"></div>                      \
                      <div id="summary_unique_count"></div>                   \
                      <br/>                                                   \
                      <table class="results" cellpadding="0" cellspacing="0"> \
                        <thead id="summary_head">' + headerText +'</thead>    \
                        <tbody id="summary_table">' + bodyText + '</tbody>    \
                      </table>                                                \
                    </div>';

        var summaryLoadedElement = document.getElementById('summary_loaded');
        summaryLoadedElement.innerHTML = html;
        var uniqueCountQid = str[1];
        setTimeout("updateCountInColumnSummary()", 200);
        setTimeout("updateUniqueCountInColumnSummary(" + uniqueCountQid + ")", 300);
    });
}

function getResultsPoller(qid, timeout, userCallback, userData) {
    var callback = function(results) {
        if (results == null) {
            // try again
            getResults(qid, timeout, userCallback, userData);
        } else {
            if (!userCallback(results, userData)) {
                getResults(qid, timeout, userCallback, userData);
            }
        }
    }

    AjaxServices.getResults(qid, callback);
}

function getResults(qid, timeout, userCallback, userData) {
    setTimeout("getResultsPoller(" + qid + ", " + timeout + ", " + userCallback + ","
               + userData + ")", timeout);
}

var callId = 0;

// Give each call an id and remember what our current pending id is
var currentFilterCallbacks = new Array();

// call AjaxServices.filterWebSearchables() then hide those WebSearchables in
// the webSearchableList that don't match
function filterWebSearchables(object, scope, type) {
    var value = object.value;
    var inputArray = document.getElementsByTagName("div");
    var pattern = new RegExp('^' + scope + '_' + type + '_item_line_(.*)');

    function showAll() {
        for(var i=0; i<inputArray.length; i++) {
            var result;
            if ((result = pattern.exec(inputArray[i].id)) != null) {
                inputArray[i].style.display='block';
                var scoreId = scope + '_' + type + '_item_score_' + result[1];
                $(scoreId).innerHTML = '';
            }
        }
        $(scope + '_' + type + '_spinner').style.visibility = 'hidden';
    }
    if (value.length > 1) {
        function filterCallBack(cbResult) {
            var callId = cbResult[0];
            var filteredList = cbResult.slice(1);

            if (currentFilterCallbacks[scope + "_" + type] != callId) {
                // we've started another call since this one started, so
                // ignore this one
                return;
            }

            if (filteredList.length == 0) {
                showAll();
            } else {
                var scoreHash = new Array();
                var descHash = new Array();
                for (var el in filteredList) {
                    var wsName = filteredList[el][0];
                    var wsScore = filteredList[el][1];
                    scoreHash[scope + '_' + type + '_item_line_' + wsName] = wsScore;
                    var wsDesc = filteredList[el][2];
                    descHash[scope + '_' + type + '_item_line_' + wsName] = wsDesc;
                }

                for(var i=0; i<inputArray.length; i++) {
                    if ((result = pattern.exec(inputArray[i].id)) != null) {
                        if (scoreHash[inputArray[i].id]) {
                            inputArray[i].style.display='block';
                            var scoreId = scope + '_' + type + '_item_score_' + result[1];
                            $(scoreId).innerHTML = scoreHash[inputArray[i].id];
                            var descId = scope + '_' + type + '_item_description_' + result[1];
                            var highlightText = descHash[inputArray[i].id];
                            $(descId).innerHTML = '<p class="description">' + highlightText + '</p>';
                        } else {
                            inputArray[i].style.display='none';
                        }
                    }
                }

                function sortWsFilter(el1, el2) {
                    var el1score = scoreHash[el1.id];
                    var el2score = scoreHash[el2.id]
                    if (el1score > el2score) {
                        return -1;
                    } else {
                        if (el1score < el2score) {
                            return 1;
                        } else {
                            return 0;
                        }
                    }
                }

                var parent = $(scope + '_' + type + '_ws_list');
                var divs = new Array();

                for (var i in parent.childNodes) {
                    var child = parent.childNodes[i];
                    if (child.tagName == 'DIV' && scoreHash[child.id]) {
                        divs.push(child);
                    }
                }

                divs.sort(sortWsFilter);

                for (var i = 0; i < divs.length; i++) {
                    parent.appendChild(divs[i]);
                }

                $(scope + '_' + type + '_spinner').style.visibility = 'hidden';
            }
        }

        $(scope + '_' + type + '_spinner').style.visibility = 'visible';
        currentFilterCallbacks[scope + "_" + type] = callId;
        AjaxServices.filterWebSearchables(scope, type, null, object.value, callId++,
                                          filterCallBack);
    } else {
        showAll();
    }
}
