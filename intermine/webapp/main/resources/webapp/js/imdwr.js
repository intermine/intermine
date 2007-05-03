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
        document.getElementById('selected_' + type + '_' + index).value=str;
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
    document.getElementById('summary_row_count').innerHTML = "<p>" + countString + "</p>";
    setTimeout("updateCountInColumnSummary()", 1000);
}

function updateUniqueCountInColumnSummary(qid) {
    var countString = document.resultsUniqueCountText;
    document.getElementById('summary_row_count').innerHTML = "<p>" + countString + "</p>";
    setTimeout("updateCountInColumnSummary()", 1000);
}

function getColumnSummary(columnName, columnDisplayName) {
    DWRUtil.removeAllRows('summary_table');
    DWRUtil.removeAllRows('summary_head');
    document.getElementById('summary_loaded').style.display = "none";
    document.getElementById('summary_loading').style.display = "block";
    Effect.Appear('summary', { duration: 0.30 });
    AjaxServices.getColumnSummary(columnName, function(str){
        var rows = str[0];
        var cellFuncs = new Array();
        var headerFuncs = new Array();
        var summaryColumnNameElement = document.getElementById('summary_column_name');
        var summaryColumnNameElement = document.getElementById('summary_column_name');
        if (summaryColumnNameElement.innerText == null) {
            // normal browsers
            summaryColumnNameElement.textContent = columnDisplayName;
        } else {
            // IE
            summaryColumnNameElement.innerText = columnDisplayName;
        }
        function makeCellFunc(index) {
            return function(data) {
                var cell = data[index];
                if (cell==null) {
                    return "[no value]" ;
                } if (! isNaN(cell)) {
                    var rf = roundToSignificantDecimal(cell+0);
                    return Math.round(cell*rf)/rf;
                } else {
                    return cell;
                }
            }
        }
        function makeHeaderFunc(index) {
            return function(data) {
                return data[index];
            }
        }
        for (var i=0;i<rows[0].length;i++){
            cellFuncs[i] = makeCellFunc(i);
            headerFuncs[i] = makeHeaderFunc(i);
        }
        var headerCreator = function(options) {
            return document.createElement("th");
        }
        var headers;
        if(cellFuncs.length == 2){
            headers = ['Value', 'Count'];
        } else if (cellFuncs.length == 4){
            headers = ['Min', 'Max', 'Sample Mean', 'Standard Deviation'];
        }
        DWRUtil.addRows('summary_head', [headers], headerFuncs, {cellCreator:headerCreator});
        DWRUtil.addRows("summary_table", rows, cellFuncs);
        document.getElementById('summary_loading').style.display = "none";
        document.getElementById('summary_loaded').style.display = "block";
        setTimeout("updateCountInColumnSummary()", 10000);
        var qid = str[1];
        setTimeout("updateUniqueCountInColumnSummary("+qid+")", 10000);
    });
}

function getResultsPoller(qid, timeout, userCallback) {
    var callback = function(results) {
        if (results == null) {
            // try again
            getResults(qid, timeout, userCallback);
        } else {
            userCallback(results);
        }
    }

    AjaxServices.getResults(qid, callback);
}

function getResults(qid, timeout, userCallback) {
    setTimeout("getResultsPoller(" + qid + ", " + timeout + ", " + userCallback + ")", timeout);
}
