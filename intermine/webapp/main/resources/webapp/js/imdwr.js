// This line keeps the nasty error message away (FireFox Bug)
//DWREngine.setMethod(DWREngine.IFrame);

var wsNamesMap = {};

function setFavourite(name, type, image){
    var img = image.src;
    var isFavourite;
    if (img.substring(img.length-12, img.length) == 'unactive.gif') {
        isFavourite = false;
    } else {
        isFavourite = true;
    }
    AjaxServices.setFavourite(name, type, isFavourite);
    // already a favourite.  turning off.
    if (isFavourite) {
        image.src='images/star_unactive.gif';
        image.title='Set as favourite';
        // not a favourite.  turn on.
    } else {
        image.src='images/star_active.gif';
        image.title='This is a favourite';
    }
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
    var uid = name + type;
    var selectionInput;
    document.getElementById('form_' + uid).style.display="none";
    document.getElementById('name_' + uid).innerHTML="<i>saving...</i>";
    document.getElementById('name_' + uid).style.display="block";
    AjaxServices.rename(name,type, (document.getElementById('newName_' + uid).value).replace(/^\s*|\s*$/g,""), function(str){
        document.getElementById('name_' + uid).innerHTML=str;
        if (document.getElementById('selected_' + type + '_' + index) != null) {
            // coming from bags/templates pages
            document.getElementById('selected_' + type + '_' + index).value=str;
        } else {
            // coming from mymine
            if (selectionInput = document.getElementById('selected_user_' + type + '_' + index)) {
                selectionInput.value = str;
            }
        }
        // reload so that new "IDs" based on the name match
        if (str.indexOf('<i>') != -1) {
            setTimeout(function() {window.location.reload();}, 1000);
        } else {
            window.location.reload();
        }
    });
}

function changeViewPathDescription(pathName){
    var pathString = pathName.replace(/\@sep\@/g,".");
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
    jQuery('textarea#textarea').toggle();
    jQuery('div#bagDescriptionDiv').toggle();
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

    jQuery('#summary_row_count').html("<p>" + countString + "</p>");

    var est = document.getElementById('resultsCountEstimate');
    if (est == null || est.style.display != 'none') {
        setTimeout("updateCountInColumnSummary()", 500);
        return;
    }
}

function updateUniqueCountInColumnSummary(uniqueCountQid) {
    getResultsSize(uniqueCountQid, 1000, resultsCountCallback, null, true);
}

function resultsCountCallback(size) {
    if (size > 1) {
        var summaryUniqueCountElement = document.getElementById('summary_unique_count');
        if(summaryUniqueCountElement != null) {
            summaryUniqueCountElement.style.display='inline';
            summaryUniqueCountElement.innerHTML='<p>Total unique values: ' + size + "</p>";
        }
    }
    return true;
}

var dialog=null;

function getColumnSummary(tableName, columnName, columnDisplayName) {
    if (dialog == null) {
        dialog = new Boxy("<img src=\"images/wait18.gif\" title=\"loading icon\" style=\"margin:25px 50px;\">&nbsp;Loading...",{title:"Column Summary", draggable: true});
    } else {
        dialog.setContent("<img src=\"images/wait18.gif\" title=\"loading icon\" style=\"margin:25px 50px;\">&nbsp;Loading...");
        if(!dialog.isVisible()) {
            dialog.show();
        }
    }
    AjaxServices.getColumnSummary(tableName, columnName, function(str){
        var rows = str[0];
        var uniqueCountQid = str[1];
        var summaryRowsCount = str[2];
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
        if (rows[0] == null) {
            headerText = '<tr><th>No results found in this column</th></tr>';
        } else if(rows[0].length == 2){
            headerText = '<tr><th>Value</th><th>Count</th></tr>';
        } else {
            headerText = '<tr><th>Min</th><th>Max</th><th>Sample Mean</th><th>Standard Deviation</th></tr>';
        }

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

        var content = '<div class="box">                                         \
                      <div class="summaryhead">                                     \
                        <h3>Column Summary for ' + columnDisplayName + '</h3>          \
                      </div>                                                  \
                      <div id="summary_row_count"></div>                      \
                      <div id="summary_unique_count"></div>                   \
                      <br/>                                                   \
                      <table class="results summary" cellpadding="0" cellspacing="0"> \
                        <thead id="summary_head">' + headerText +'</thead>    \
                        <tbody id="summary_table">' + bodyText + '</tbody>    \
                      </table>';
        if (summaryRowsCount > 10) {
            content += '<div><p>Note: showing only the first 10 rows of summary.</p></div>';
       }
       content += '<p><a href="columnSummary.do?tableName=' + tableName + '&summaryPath=' + columnName + '">View all</a></p></div>';

        dialog.setContent(content);
        setTimeout("updateCountInColumnSummary()", 200);
        setTimeout("updateUniqueCountInColumnSummary(" + uniqueCountQid + ")", 300);
   });

}

var qid, timeout, userCallback;

function getResultsPoller(qid, timeout, userCallback) {
    var callback = function(results) {
        if (results == null) {
            // try again
            getResultsSize(qid, timeout, userCallback);
        } else {
            if (!userCallback(results)) {
                getResultsSize(qid, timeout, userCallback);
            }
        }
    }

    AjaxServices.getResultsSize(qid, callback);
}

function getResultsSize(qid1, timeout1, usercallback1) {
    qid = qid1;
    timeout = timeout1;
    userCallback = usercallback1;
    //Passing variables directly doesn't work in Safari
    setTimeout("getResultsPoller(qid, timeout, userCallback, true)", timeout);
}

// not needed for now:
// function getResults(qid1, timeout1, usercallback1) {
//  qid = qid1;
//  timeout = timeout1;
//  userCallback = usercallback1;
//  //Passing variables directly doesn't work in Safari
//     setTimeout("getResultsPoller(qid, timeout, userCallback, false)", timeout);
// }

// function getResults(qid1,timeout1,usercallback1) {
//  qid = qid1;
//  timeout = timeout1;
//  userCallback = usercallback1;
//  //Passing variables directly doesn't work in Safari
//     setTimeout("getResultsPoller(qid, timeout, userCallback, false)", timeout);
// }

// delete all the child nodes of the node given by parentElement and create a child
// element of type childTag, setting the text in the new child element to be
// childText
function setChild(parentElement, childText, childTag) {
    var newChild = document.createElement(childTag);
    newChild.innerHTML = childText;
    if (parentElement.firstChild != null) {
        while (parentElement.hasChildNodes()) {
            parentElement.removeChild(parentElement.firstChild);
        }
    }
    parentElement.appendChild(newChild);
    return newChild;
}

var callId = 1;

// Give each ajax call an id and remember what our current pending id is
var currentFilterCallbacks = new Array();

// Give each setTimeout call an id and remember what our current pending id is
var futureFilterCalls = new Array();

function filterWebSearchablesHandler(event, object, type, wsListId) {
    var scope = document.getElementById('filterScope_'+wsListId+'_'+type).value;
    if (window.event) {
        event = window.event;
    }
    if (event) {
        if (event.keyCode == 27) {
            object.value = '';
            clearFilter(type, wsListId);
            return;
        }
        if (event.keyCode == 13
            || event.keyCode == 16
            || event.keyCode == 17
            || event.keyCode == 33
            || event.keyCode == 34
            || event.keyCode == 35
            || event.keyCode == 36
            || event.keyCode == 37
            || event.keyCode == 38
            || event.keyCode == 39
            ||event.keyCode ==  40) {
            return false;
        }
    }

    futureFilterCalls[wsListId + "_" + type] = callId;

    if (tags == null) {
        tags = [];
    }
    // if (object.value=='') {
    //     showAll(wsListId,type);
    // }

    setTimeout('filterWebSearchables("' + object.id + '", "' + scope + '","' + type + '","' + callId + '","' + wsListId + '")', 500);
    callId++;
}

// given a list of WebSearchables names,scores and descriptions, filter the
// wsFilterList given by the wsListId and type parameters
function do_filtering(filteredList, type, wsListId) {
    setItemsFiltered(true);
    if (filteredList.length == 0) {
        document.getElementById(wsListId+'_'+type+'_no_matches').style.display='block';
        $(wsListId + '_' + type + '_spinner').style.display = 'none';
        $(wsListId + '_' + type + '_container').style.display = 'none';
    } else {
        var scoreHash = new Array();
        var descHash = new Array();
        var hitHash = new Array();

        for (var el in filteredList) {
            var wsName = filteredList[el][0];
            var wsDesc = filteredList[el][1];
            descHash[wsListId + '_' + type + '_item_line_' + wsName] = wsDesc;
            var wsScore = filteredList[el][2];
            scoreHash[wsListId + '_' + type + '_item_line_' + wsName] = wsScore;
            hitHash[wsListId + '_' + type + '_item_line_' + wsName] = 1;
        }

        for(var name in wsNamesMap) {
            var div = wsNamesMap[name];
            if (hitHash[div.id]) {
                div.style.display='block';
                var highlightText = descHash[div.id];

                if (highlightText) {
                    var descId = wsListId + '_' + type + '_item_description_' + name;
                    var desc = $(descId);
                    desc.style.display = 'none';

                    var descHighlightId = wsListId + '_' + type + '_item_description_' + name + '_highlight';
                    var descHighlight = $(descHighlightId);
                    descHighlight.style.display = 'block';

                    var descChild = setChild(descHighlight, highlightText, 'p');
                    if(scoreHash.length == 0) {
                        var scoreId = wsListId + '_' + type + '_item_score_' + name;
                        var scoreSpan = $(scoreId);
                        // we do this instead of scoreSpan.innerHTML = "stuff"
                        // because it's buggy in Internet Explorer
                        setChild(scoreSpan, '', 'span');
                    }
                }

                if (scoreHash[div.id]) {
                    var scoreWsName = name;
                    var score = scoreHash[div.id];
                    var intScore = parseInt(score * 10);
                    var scoreId = wsListId + '_' + type + '_item_score_' + scoreWsName;
                    var scoreSpan = $(scoreId);
                    // we do this instead of scoreSpan.innerHTML = "stuff"
                    // because it's buggy in Internet Explorer
                    var heatImage = 'heat' + intScore + '.gif';
                    var heatText = '<img height="10" width="' +
                        (intScore * 3) + '" src="images/' + heatImage + '"/>';
                    setChild(scoreSpan, heatText, 'span');
                }
            } else if (document.getElementById(wsListId + '_' + type + '_chck_' + name).checked != true){
               div.style.display='none';
            }
        }

        showWSList(wsListId, type);

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

        var parent = $(wsListId + '_' + type + '_ws_list');
        var divs = new Array();

        for (var i = 0; i < parent.childNodes.length; i++) {
            var child = parent.childNodes[i];
            if (child.tagName == 'DIV' && scoreHash[child.id]) {
                divs.push(child);
            }
        }

        divs.sort(sortWsFilter);

        for (var i = 0; i < divs.length; i++) {
            parent.appendChild(divs[i]);
        }

        $(wsListId + '_' + type + '_spinner').style.display = 'none';
        $(wsListId + '_' + type + '_container').style.display = 'block';
        showDescriptions(wsListId, type, isDescriptionShown());
    }
}

function showElement(el, show) {
    if (el) {
        if (show) {
            el.style.display = 'block';
        } else {
            el.style.display = 'none';
        }
    }
}

descriptionShown = true;

function showDescriptions(listId, type, show) {
    descriptionShown = show;
    var prefix = listId + '_' + type + '_item_description';
    var divs = document.getElementsByTagName('div');
    var i = 0;
    for (i = 0; i < divs.length; i++) {
        var el = divs[i];
        // it is div with description
        if (el.id.match(prefix) != null) {
            // there are 2 descriptions: normal and highlighted
            // Results returned from Lucene search are crazy, sometimes the highlighted
            // description is empty after Lucene search and that's why this complicated
            // logic is needed - normal description is hidden if the highlighted is not empty
            if (areItemsFiltered()) {
                if (el.id.match('_highlight') != null) {
                    showElement(el, show);
                } else {
                    if (document.getElementById(el.id + '_highlight').innerHTML.length != 0) {
                        showElement(el, false);
                    }
                }
            } else {
                if ((el.id.match('_highlight') != null)) {
                    showElement(el, false);
                } else {
                    showElement(el, show);
                }
            }
        }
    }
    AjaxServices.setState(prefix, show);
}

// un-hide all the rows in the webSearchableList
function showAll(wsListId, type) {
    for(var name in wsNamesMap) {
        var div = wsNamesMap[name];

        div.style.display='block';
        var scoreId = wsListId + '_' + type + '_item_score_' + name;
        var scoreSpan = $(scoreId);
        // we do this instead of scoreSpan.innerHTML = "stuff"
        // because it's buggy in Internet Explorer
        setChild(scoreSpan, '', 'span');

        var descId = wsListId + '_' + type + '_item_description_' + name;
        var desc = $(descId);
        desc.style.display = 'block';

        var descHighlightId = wsListId + '_' + type + '_item_description_' + name + '_highlight';
        var descHighlight = $(descHighlightId);
        descHighlight.style.display = 'none';
    }

    $(wsListId + '_' + type + '_spinner').style.display = 'none';
    $(wsListId + '_' + type + '_container').style.display = 'block';
    $(wsListId + '_' + type + '_no_matches').style.display='none';
}

itemsFiltered = false;

function areItemsFiltered() {
    return itemsFiltered;
}

function setItemsFiltered(filtered) {
    itemsFiltered = filtered;
}

function isDescriptionShown() {
    if (descriptionShown) {
        return descriptionShown;
    } else {
        return false;
    }
}

// call AjaxServices.filterWebSearchables() then hide those WebSearchables in
// the webSearchableList that don't match
function filterWebSearchables(objectId, scope, type, callId, wsListId) {
    if (futureFilterCalls[wsListId + "_" + type] != callId) {
        // filterWebSearchablesHandler() has been called again since this
        // timeout was set, so ignore as another timeout will be along
        // shortly
        return;
    }

    var object = document.getElementById(objectId);
    var value = object.value;
    var filterAction = document.getElementById('filterAction' + '_' + wsListId + '_' + type).value;


    if ( (value != null) || (tags != null && tags.length > 1)) {
        function filterCallBack(cbResult) {
            var callId = cbResult[0];
            var filteredList = cbResult.slice(1);

            if (currentFilterCallbacks[wsListId + "_" + type] != callId) {
                // we've started another call since this one started, so
                // ignore this one
                return;
            }

            do_filtering(filteredList, type, wsListId);
        }
        $(wsListId+'_'+type+'_no_matches').style.display='none';
        $(wsListId + '_' + type + '_spinner').style.display = 'block';
        $(wsListId + '_' + type + '_container').style.display = 'none';
        currentFilterCallbacks[wsListId + "_" + type] = callId;
        var tagList = null;

        /*
          We need to transform our map into a proper Array
        */
        tagList = new Array();
        if(tags['favourites_' + wsListId] != null && tags['favourites_' + wsListId] != '') {
            tagList[tagList.length]=tags['favourites_' + wsListId];
        } if(tags['aspects_' + wsListId] != null && tags['aspect_' + wsListId] != '') {
            tagList[tagList.length]=tags['aspects_' + wsListId];
        }
        if (typeof selectedUserTag != "undefined" && selectedUserTag != null) {
            tagList[tagList.length] = selectedUserTag;
        }

        /*  filterAction toggles favourites off and on */
        AjaxServices.filterWebSearchables(scope, type, tagList, object.value, filterAction,
                                          callId++, filterCallBack);
    } else {
        showAll(wsListId, type);
    }

    futureFilterCalls[wsListId + "_" + type] = 0;
}

var tags = new Array();

function filterFavourites(type, wsListId) {
    var id = 'filterAction_'+wsListId+'_'+type;
    // var tags = '';
    var scope = document.getElementById('filterScope_'+wsListId+'_'+type).value;

    // favourites OFF
    if(document.getElementById(id).value == "favourites") {
        document.getElementById(id).value = "";
        document.getElementById('filter_favourites_'+wsListId+'_'+type).src = 'images/filter_favourites.png';
        delete tags['favourites_' + wsListId];
    // favourites ON
    } else {
        document.getElementById(id).value = "favourites";
        document.getElementById('filter_favourites_'+wsListId+'_'+type).src = 'images/filter_favourites_active.png';
        tags['favourites_' + wsListId] = 'im:favourite';
    }
    var filterTextElement = document.getElementById('filterText');
    return filterWebSearchablesHandler(null, filterTextElement, type, wsListId);
}

function filterAspect(type, wsListId) {
    var id = 'filterAction_'+wsListId+'_'+type;
    var aspect = document.getElementById(wsListId+'_'+type+'_filter_aspect').value;
    var scope = document.getElementById('filterScope_'+wsListId+'_'+type).value;

    // aspects ON
    if(aspect != null && aspect.length > 1) {
        aspect = 'im:aspect:'+ aspect;
        tags['aspects_' + wsListId] = aspect;
    } else {
        delete tags['aspects_' + wsListId]
    }

    var filterTextElement = document.getElementById('filterText');
    return filterWebSearchablesHandler(null, filterTextElement, type, wsListId);
}

function filterByUserTag(type, wsListId, tag) {
    // it is checked in filterWebSearchablesHandler
    selectedUserTag = tag;

    // boring stuff to reload new filtered web searchables from server
    var filterTextElement = document.getElementById('filterText');
    return filterWebSearchablesHandler(null, filterTextElement, type, wsListId);
}


function changeScope(type, wsListId) {
    var id = 'filterScope_'+wsListId+'_'+type;
    var scope = document.getElementById(id).value;

    if(scope == 'all') {
        document.getElementById(id).value = 'user';
        document.getElementById('filter_scope_'+wsListId+'_'+type).src = 'images/filter_my_active.png';
    } else if(scope == 'user') {
        document.getElementById(id).value = 'all';
        document.getElementById('filter_scope_'+wsListId+'_'+type).src = 'images/filter_all.png';
    }
    var filterTextElement = document.getElementById('filterText');
    return filterWebSearchablesHandler(null, filterTextElement, type, wsListId);
}

function clearFilter(type, wsListId) {
    setItemsFiltered(false);
    var scopeId = 'filterScope_'+wsListId+'_'+type;
    var favId = 'filterAction_'+wsListId+'_'+type;
    var aspectId = wsListId+'_'+type+'_filter_aspect';

    document.getElementById(scopeId).value = 'all';
    var scopeElement = document.getElementById('filter_scope_'+wsListId+'_'+type);
    if (scopeElement != null) {
        scopeElement.src = 'images/filter_all.png';
    }
    delete tags['favourites_' + wsListId];

    document.getElementById(favId).value = "";
    var favElement = document.getElementById('filter_favourites_'+wsListId+'_'+type);
    if (favElement != null) {
        favElement.src = 'images/filter_favourites.png';
    }

    delete tags['aspects_' + wsListId];

    if ($(aspectId) != null) {
        $(aspectId).value = '';
    }
    var filterTextElement = document.getElementById('filterText');
    filterTextElement.value = '';

    showAll(wsListId, type);
    var checkbox = document.getElementById("showCheckbox");
    if  (checkbox) {
        checkbox.checked = 'checked';
    }
    return false;
}

function setWsNamesMap(wsNames, wsListId, type) {
   // initialise wsNamesMap so that the values are the corresponding DIV
   // elements
   for (var name in wsNames) {
     wsNamesMap[name] = $(wsListId + '_' + type + '_item_line_' + name);
   }

   1;
}

// used on list analysis page
function getConvertCountForBag(bagName, type, idname) {
    AjaxServices.getConvertCountForBag(bagName, type, function(count) {
        dwr.util.setValue(type + '_convertcount_'+idname, count)
    });
}

// I don't think this is used anymore
function getURL(bagName, type, idname) {
    AjaxServices.getConvertCountForBag(bagName, type, function(count) {
        dwr.util.setValue(type + '_convertcount_'+idname, count)
    });
}

function getCustomConverterCounts(bagName, converter, callback) {
    AjaxServices.getCustomConverterCounts(bagName, converter, function(resultsArray) {
      callback.call(window, resultsArray);
    });
}

function saveToggleState(elementId) {
    var display = document.getElementById(elementId).style.display;
    var opened;
     if(display=='none') {
        opened = false;
     } else {
        opened = true;
     }
    AjaxServices.saveToggleState(elementId, opened);
}

// historyBagView.jsp, wsFilterList.jsp
function validateBagOperations(formName, operation) {
    if (Event && (Event.keyCode == 13
                  || Event.keyCode == 33
                  || Event.keyCode == 34
                  || Event.keyCode == 35
                  || Event.keyCode == 36
                  || Event.keyCode == 37
                  || Event.keyCode == 38
                  || Event.keyCode == 39
                  || Event.keyCode ==  40)) {
        return;
    }

    var bagName = '';
    var frm = document.forms[formName];
    if (frm.newBagName) {
        bagName = frm.newBagName.value;
    }
    var selectedBags = [];
    var i = 0;
    var j = 0;

    if (frm.selectedBags) {
        // if there is only one item checked, then javascript doesn't work with it as a with array
        if (!frm.selectedBags.length) {
            selectedBags[0] = frm.selectedBags.value;
        } else {
            for (i = 0; i < frm.selectedBags.length; i++){
                if (frm.selectedBags[i].checked) {
                    selectedBags[j] = frm.selectedBags[i].value;
                    j++;
                }
            }
        }
    }

    AjaxServices.validateBagOperations(bagName, selectedBags, operation, function(errMsg) {
        if (errMsg != '') {
            var msgBagInUse = "You are trying to delete the list";
            // if the list they are trying to delete is in use, prompt for response
            // then delete list if the user clicks OK
            if (operation == 'delete' && errMsg.substring(0,33) == msgBagInUse) {
                Boxy.confirm(errMsg, function() {
                    frm.listsButton.value = operation;
                    frm.submit();
                }, {title: 'Warning', modal: false});
            } else {
                Boxy.alert(errMsg, null, {title: 'Error', modal: false});
            }
        } else {
            jQuery('table.boxy-wrapper').hide();
            frm.listsButton.value = operation;
            frm.submit();
        }
    });

    return false;
}

// table.jsp, bagUploadConfirm.jsp
function validateBagName(formName) {
    if (Event && (Event.keyCode == 33
                  || Event.keyCode == 34
                  || Event.keyCode == 35
                  || Event.keyCode == 36
                  || Event.keyCode == 37
                  || Event.keyCode == 38
                  || Event.keyCode == 39
                  || Event.keyCode ==  40)) {
        return;
    }

    var frm = document.forms[formName];

    var bagName = frm.newBagName.value;

    AjaxServices.validateBagName(bagName, function(errMsg) {
        if (errMsg != '') {
            jQuery('#bigGreen').removeClass('clicked');
            var newError = jQuery('<div class="error-message">' + errMsg + "</div>");
            var errorContainer = jQuery('#error_msg');
            errorContainer.fadeOut('fast', function() {
                errorContainer.find('.error-message').remove();
                errorContainer.append(newError).fadeIn('fast');
            });
        } else {
            if (frm.operationButton) {
                frm.operationButton.value="saveNewBag";
            }
            frm.submit();
        }
   });
}

/*function switchJoin(element) {
   var pathName = element.id.replace('join_arrow_','');
   var elementid = element.id;
   AjaxServices.setOuterJoin(pathName,function(newPathName){
        // replace all children ids with the updated path
     jQuery.each(jQuery('.joinLink'), function(index, item) {
       if(item.id.match("^" + elementid)) {
         item.id = item.id.replace(pathName, newPathName);
       }
     });
        reDrawConstraintLogic();
   });
   if(jQuery(element).attr('src').indexOf('hollow')>-1) {
        jQuery(element).attr('src','images/join_full.png');
   } else {
        jQuery(element).attr('src','images/join_hollow.png');
   }
}*/

function setConstraintLogic(expression) {
    AjaxServices.setConstraintLogic(expression, function(messages) {
    if (messages != "") {
      jQuery('#msg').append(messages);
      jQuery('#msg').fadeIn(2000);
    }
    reDrawConstraintLogic();
    jQuery('#constraintLogic').toggle();
    jQuery('#editConstraintLogic').toggle();
  });
}

function reDrawConstraintLogic() {
  AjaxServices.getConstraintLogic(function(expression) {
      expression = expression.replace('[','').replace(']','').replace(',',' and');
      jQuery('#constraintLogic').text(expression);
      jQuery('span#editConstraintLogic input#expr').val(expression);
  });
}

/** @deprecated */
function refreshSavedBagStatus() {
    AjaxServices.getSavedBagStatus(function(savedBagStatus) {
        var allCurrent = true;
        if (savedBagStatus) {
            var jSONObject = jQuery.parseJSON(savedBagStatus);
            jQuery.each(jSONObject, function(key, entry) {
                var bagName = entry['bagName'];
                var status = entry['status'];
                if (status == 'NOT_CURRENT' || status == 'UPGRADING')
                    allCurrent = false;
                document.getElementById("status_" + bagName).innerHTML = getHTML(status, bagName);
                document.getElementById("size_" + bagName).innerHTML = entry['size'];
                if (status == 'CURRENT') {
                    document.getElementById("linkBag_" + bagName).innerHTML = '<a href="bagDetails.do?bagName=' + bagName + '">' + bagName + '</html:link>';
                    hrefEdit='<a href="javascript:editName(\'' + bagName + '\');"><img border="0" src="images/edit.gif" width="13" height="13" title="Click here to rename this item"/></a>';
                    document.getElementById("editName_" + bagName).innerHTML = hrefEdit;
                }
                if (status == 'TO_UPGRADE') {
                    document.getElementById("linkBag_" + bagName).innerHTML = '<a href="bagUpgrade.do?bagName=' + bagName + '" class="bagToUpgrade">' + bagName + '</html:link>';
                }
            })
            if (!allCurrent) {
                setTimeout('refreshSavedBagStatus()', 1000);
            }
        }
    });
}

function getHTML(status, bagName) {
    if (status == 'CURRENT')
      return "Current";
    else if (status == 'NOT_CURRENT')
      return "Not current";
    else if (status == 'UPGRADING')
      return "Upgrading...";
    else if (status == 'TO_UPGRADE')
      return "<a href='bagUpgrade.do?bagName=" + bagName + "' class='bagToUpgrade'>Upgrade</html:link>";
}

function updateTemplate(field, value) {
    AjaxServices.updateTemplate(field, value);
}
