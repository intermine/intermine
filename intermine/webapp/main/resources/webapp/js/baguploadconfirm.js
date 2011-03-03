var duplicateArray = new Array();
var tdColorArray = new Array();
var highlightColor = '#FFF3D3';

/**
 * Add an identifier into the bag
 * @param objectId of the actual row
 * @param row number relative to the table in question
 * @param parentId is the identifier we were trying to upload
 * @param issueType, e.g.: 'duplicate' etc.
 * @return
 */
function addId2Bag(objectId, row, parentId, issueType) {
  // I can remove now...
  setLinkState('removeAllLink', 'active');

  if (jQuery('#add_' + issueType + '_' + objectId).hasClass('fakelink')) {
    // switch the class on add/remove links
    jQuery('#add_' + issueType + '_' + objectId).removeClass('fakelink');
    jQuery('#rem_' + issueType + '_' + objectId).addClass('fakelink');

        // colour the row columns
        colourRow('row_' + issueType + '_' + row, true);

        // remove identifier from a list of additional matches to upload
        addIdToListOfMatches(objectId);

        // update the number of matches count
        updateCount('#matchCount', 1);

        var idArray = duplicateArray[parentId];
        if (idArray == null) {
          duplicateArray[parentId] = new Array(objectId);

          // decrease count
          updateCount('#' + issueType + 'Count', -1);

          // apply bg color to identifier
          jQuery('#td_' + issueType + '_' + parentId).css('background-color', highlightColor);

          // decrease count
          updateCount('#initialIdCount', -1);
        } else {
          idArray[idArray.length] = objectId;
          duplicateArray[parentId] = idArray;
        }

        setLinkState(issueType+'removeAllLink', 'active');
    }
}

/**
 * Set color on row elements
 * @param rowClass
 * @param highlighted true if we are highlighting now
 * @return
 */
function colourRow(rowClass, highlighted) {
  // match all 'row_duplicate_0' elements
  jQuery('td.' + rowClass).each(function(index) {
    // are we highlighting?
    if (highlighted) {
      tdColorArray[rowClass] = jQuery(this).css('background-color');
      jQuery(this).css('background-color', highlightColor);
    } else {
      // set color back (transparent)
      jQuery(this).css('background-color', tdColorArray[rowClass]);
    }
  });

}

/**
 * Will update a count in an element, checks if element exists first
 * @param element jQuery syntax
 * @param amount integer
 * @return
 */
function updateCount(element, amount) {
  // exists?
  if (jQuery(element).length > 0) {
    // parse value
    var count = parseInt(jQuery(element).text()) + amount;
    // set value
    jQuery(element).text(count);
  }
}

/**
 * Will add an identifier to a list of matches in #matchIDs hidden input element
 * @param identifier we were trying to upload
 * @return
 */
function addIdToListOfMatches(identifier) {
  var bagList = jQuery('input#matchIDs').val();

  if (bagList.indexOf(identifier) == -1) {
    // add an extra space if we have elements inside already
    if (bagList.length > 0) {
      bagList += " ";
    }
    jQuery('input#matchIDs').val(bagList + identifier);
  }
}

/**
 * Will remove an identifier from a list of matches in #matchIDs hidden input element
 * @param identifier we were trying to upload
 * @return
 */
function removeIdFromListOfMatches(identifier) {
  // get the list of identifiers
  var bagList = jQuery("#matchIDs").val();

  // if we have found the identifier in the string
  if (bagList.indexOf(identifier) > -1) { // jQuery.inArray() not consistent...
    // remove the identifier from an array
    bagList = jQuery.grep(bagList.split(' '), function(value) { return value != identifier; });
    // turn array into string and replace all "," with a space
    jQuery("#matchIDs").val(bagList.toString().replace(new RegExp(",","g"), ' '));
  } else {
    // trying to remove an element that is not in the list
  }
}

/**
 * Remove identifier from the bag
 * @param objectId of the actual row
 * @param row number relative to the table in question
 * @param parentId is the identifier we were trying to upload
 * @param issueType, e.g.: 'duplicate' etc.
 * @return
 */
function removeIdFromBag(objectId, row, parentId, issueType) {
    setLinkState('addAllLink', 'active');

    if (jQuery('#rem_' + issueType + '_' + objectId).hasClass('fakelink')) {
        // switch the class on remove/add links
        jQuery('#rem_' + issueType + '_' + objectId).removeClass('fakelink');
        jQuery('#add_' + issueType + '_' + objectId).addClass('fakelink');

        // switch off row color
        colourRow('row_' + issueType + '_' + row, false);

        // remove identifier from a list of additional matches to upload
        removeIdFromListOfMatches(objectId);

        // update the number of matches count
        updateCount('#matchCount', -1);

        var idArray = duplicateArray[parentId];
        if (idArray.length == 1) {
            // increase count
            updateCount('#' + issueType + 'Count', 1);

            // 'remove' background color from identifier
            jQuery('#td_' + issueType + '_' + parentId).css('background-color', 'transparent');

            // reduce count
            updateCount('#initialCount', -1);

            duplicateArray[parentId] = null;
        } else {
            var idArrayCopy = new Array();

            jQuery.each(idArray, function(i, value) {
              if (objectId != value) {
                idArrayCopy[idArrayCopy.length] = idArray[i];
              }
            });

            duplicateArray[parentId] = idArrayCopy;
        }

        setLinkState(issueType + 'addAllLink', 'active');
    }
}

function addAll(issue, flatArray){
  // split string into rows
  // a,b,c,d|e,f,g,h
  var a = flatArray.split("|");
    if (a.length > 1000 || (a.length > 200 && BrowserDetect.browser == 'Explorer')) {
        var r = window.confirm('There are many items in the table. This operation can take a while. Please be patient and do not stop script or cancel it now.');
        if (! (r == true)) {
            return;
        }
    }
  for (i = 0; i < a.length -1; i++) {
    // split rows into vars
    var b = a[i].split(",");
    addId2Bag(b[0], b[1], b[2], b[3]);
  }
  toggleBagLinks(issue, 'add');
}

function removeAll(issue, flatArray){
  // split string into rows
  // a,b,c,d|e,f,g,h
  var a = flatArray.split("|");
    if (a.length > 1000 || (a.length > 500 && BrowserDetect.browser == 'Explorer')) {
        var r = window.confirm('There are many items in the table. This operation can take a while. Please be patient and do not stop script or cancel it now.');
        if (! (r == true)) {
            return;
        }
    }
  for (i = 0; i < a.length -1; i++) {
    // split rows into vars
    var b = a[i].split(",");
    removeIdFromBag(b[0], b[1], b[2], b[3]);
  }
  toggleBagLinks(issue, 'remove');
}

function toggleBagLinks(issue, action) {

  toggleBagLink(issue, action);
  if (issue == 'all') {
    toggleBagLink('lowQ', action);
    toggleBagLink('duplicate', action);
    toggleBagLink('converted', action);
  }
}

function toggleBagLink(issue, action) {

  var addAllLink = 'addAllLink';
  var removeAllLink = 'removeAllLink';

  if (issue != 'all') {
    addAllLink = issue + addAllLink;
    removeAllLink = issue + removeAllLink;
  }
  if (action == 'remove') {
    setLinkState(addAllLink, 'active');
    setLinkState(removeAllLink, 'passive');
  } else {
    setLinkState(addAllLink, 'passive');
    setLinkState(removeAllLink, 'active');
  }

}

/**
 * Switch between the state of the 'links' that add/remove identifiers
 * @param link
 * @param state
 * @return
 */
function setLinkState(link, state) {
  if (jQuery('#' + link).length > 0) {
    if (state == 'active') {
      jQuery('#' + link).addClass("fakelink");
    } else {
      jQuery('#' + link).removeClass("fakelink");
    }
  }

}

