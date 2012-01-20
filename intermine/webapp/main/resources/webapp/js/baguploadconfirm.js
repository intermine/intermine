var duplicateArray 		= new Array(),
  tdColorArray   		= new Array(),
  highlightColor 		= '#FFF3D3',
  bagList 			= jQuery('input#matchIDs').val(),
  identifiersInTheBag = bagList.split(" ");

function initForm(buildNewBag) {
    if (buildNewBag == null || buildNewBag != 'true') {
        jQuery("#newBagName").attr('disabled', 'disabled');
    }
}

/**
 * Turn bagList back to its input field form
 */
function updateMatchIDs() {
  if (bagList.length > 0) {
    jQuery('input#matchIDs').val(bagList);
    return true;
  }
  return false;
}

/**
 * Run a function checking if we already have items in the bag
 */
function checkIfAlreadyInTheBag() {
    // run a function checking if we already have items in the bag
    jQuery('span.fakelink').each(function(index) {
      // get the element id
      var id = jQuery(this).attr("id");
      // parse out the actual identifier
      var identifier = id.substring(id.lastIndexOf("_") + 1);

      // check if we have it
      if (isIdentifierInTheBag(identifier)) {
        // ...then select it
        jQuery(this).click();
        // ...and remove the controls
        jQuery(this).parent().html("<p>Already in your list.</p>")
      }
    });
}

/**
 * check if value is already in a bag
 * @param identifier, actual object ID
 * @returns {Boolean}
 */
function isIdentifierInTheBag(identifier) {
  var array = identifiersInTheBag;
  for (var i = 0; i < array.length; i++) {
    if (identifier == array[i]) {
      return true;
    }
  }
  return false;
}

/**
 * We cannot rely on JSP to give us an accurate number of matches we have
 * in a bag anymore, thus run this method to count the number of rows
 * highlighted (= used)
 */
function hasUnhighlightedRows() {
  var unhighlighted = false;
  jQuery("#additionalMatches table.inlineResultsTable tbody tr").each(function(index) {
    if (jQuery(this).find('td:not(.identifier)').first().attr('style') != 'background-color: rgb(255, 243, 211);') {
      unhighlighted = true;
      return false;
    }
  });
  return unhighlighted;
}

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
      jQuery('span#add_' + issueType + '_' + objectId).removeClass('fakelink');
      jQuery('span#rem_' + issueType + '_' + objectId).addClass('fakelink');

      // colour the row columns (even associated rows, based on object id)
      jQuery('span#add_' + issueType + '_' + objectId).each(function(i) {
        var rowId = jQuery(this).parent().attr('class').split(" ")[1];
        colourRow(rowId, true);
      });

      if (!isIdentifierInTheBag(objectId)) {
      identifiersInTheBag.push(objectId);

        // update the number of matches count
        updateCount('#matchCount', 1);

        // now update the count of items in a JS var from bagUploadConfirm.jsp
        if (matchCount != null) {
          matchCount++;
        }
      }

      // remove identifier from a list of additional matches to upload
      addIdToListOfMatches(objectId);

      var idArray = duplicateArray[parentId];
      if (idArray == null) {
        duplicateArray[parentId] = new Array(objectId);

        // decrease count
        updateCount('#' + issueType + 'Count', -1);

        // apply bg color to identifier(s)
        jQuery('span#add_' + issueType + '_' + objectId).each(function(i) {
          var tdId = jQuery(this).parent().parent().attr('id').substring(3);
          jQuery('td#td_' + issueType + '_' + tdId).addClass('highlight');
        });

        // decrease count
        updateCount('#initialIdCount', -1);
      } else {
        idArray[idArray.length] = objectId;
        duplicateArray[parentId] = idArray;
      }

      setLinkState(issueType + 'removeAllLink', 'active');

      // update the text of the button that saves our list
      updateFurtherMatchesDisplay();
    } else {
      // update the color anyways, we are already included through another object
      //jQuery('span#add_' + issueType + '_' + objectId).parent().parent().css('background-color', highlightColor);
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
    jQuery('span#rem_' + issueType + '_' + objectId).removeClass('fakelink');
    jQuery('span#add_' + issueType + '_' + objectId).addClass('fakelink');

    // decolour the row columns (even associated rows, based on object id)
    jQuery('span#add_' + issueType + '_' + objectId).each(function(i) {
      var rowId = jQuery(this).parent().attr('class').split(" ")[1];
      colourRow(rowId, false);
    });

    if (isIdentifierInTheBag(objectId)) {
      // remove the item from the identifiers
      identifiersInTheBag.splice(identifiersInTheBag.indexOf(objectId), 1);

      // update the number of matches count
      updateCount('#matchCount', -1);

      // now update the count of items in a JS var from bagUploadConfirm.jsp
      if (matchCount != null) {
        matchCount--;
      }
    }

    // remove identifier from a list of additional matches to upload
    removeIdFromListOfMatches(objectId);

    var idArray = duplicateArray[parentId];
    if (idArray != null) {
      if (idArray.length == 1) {
        // increase count
        updateCount('#' + issueType + 'Count', 1);

        // 'remove' background color from identifier
        jQuery('span#add_' + issueType + '_' + objectId).each(function(i){
          var tdId = jQuery(this).parent().parent().attr('id').substring(3);
          jQuery('td#td_' + issueType + '_' + tdId).removeClass('highlight');
        });

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
    }

    setLinkState(issueType + 'addAllLink', 'active');

    // update the text of the button that saves our list
    updateFurtherMatchesDisplay();
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
 * @deprecated possibly... :)
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
 * Will check the current number of matches vs the total count and appropriately show info message
 *  and update form button text with the totals
 * @return
 */
function updateFurtherMatchesDisplay() {
  // update the text of the button that saves our list
  if (jQuery('input#saveList').length > 0) {
    if (matchCount > 0) {
      // button style
      jQuery('input#saveList').parent().removeClass('inactive');
      // button text
      if (matchCount > 1) {
        jQuery('input#saveList').val("Save a list of " + matchCount + " " + listType + "s");
      } else {
        jQuery('input#saveList').val("Save a list of " + matchCount + " " + listType);
      }
    } else {
      // button style
      jQuery('input#saveList').parent().addClass('inactive');
      // button text
      jQuery('input#saveList').val("0 " + listType);
    }
    // "further matches" text
    if (jQuery('p#furtherMatches').length > 0) {
      if (hasUnhighlightedRows()) {
        jQuery('p#furtherMatches').html(furtherMatchesText);
      } else {
        jQuery('p#furtherMatches').html(null);
      }
    }
  }
}

function unHighlightRow(rowId) {
    colourRow(rowId, false);
}

/**
 * Will add an identifier to a list of matches in #matchIDs hidden input element
 * @param identifier we were trying to upload
 * @return
 */
function addIdToListOfMatches(identifier) {
  if (bagList.indexOf(identifier) == -1) {
    // add an extra space if we have elements inside already
    if (bagList.length > 0) {
      bagList += " ";
    }
    bagList += identifier;
  }
}

/**
 * Will remove an identifier from a list of matches in #matchIDs hidden input element
 * @param identifier we were trying to upload
 * @return
 */
function removeIdFromListOfMatches(identifier) {
  // if we have found the identifier in the string
  if (bagList.indexOf(identifier) > -1) { // jQuery.inArray() not consistent...
    // remove the identifier from an array
    bagList = jQuery.grep(bagList.split(' '), function(value) { return value != identifier; });
    // turn array into string and replace all "," with a space
    bagList = bagList.toString().replace(new RegExp(",","g"), ' ');
  } else {
    // trying to remove an element that is not in the list
  }
}

function addAll(issue, flatArray){
  // split string into rows
  // a,b,c,d|e,f,g,h
  var a = flatArray.split("|");
  if (a.length > 100) {
      var r = window.confirm('There are many items in the table. This operation can take a while. Please be patient and do not stop script or cancel it now.');
      if (! (r == true)) {
          return;
      }
      // show a loading message that the identifiers are being resolved
      jQuery('#error_msg.topBar.errors').clone().addClass('loading').attr('id', 'addingIdentifiers')
      .html('<p>Additional matches are being resolved, please wait.</p>')
      .appendTo(jQuery("#error_msg.topBar.errors").parent()).show();
  }

  // loading img
  jQuery('<div/>', {
    'class': 'loading'
  }).appendTo(jQuery("#sidebar ul li." + issue));

    jQuery.each(a, function(i, v) {
      // use a queue for long running code
      im.queue.put(function() {
        // split rows into vars
        var b = v.split(",");
        addId2Bag(b[0], b[1], b[2], b[3]);
      }, this);
    });

    // queue in switching of links and message
    im.queue.put(function() {
      jQuery("#sidebar ul li." + issue + ' div.loading').remove();
        toggleBagLinks(issue, 'add');
        jQuery('#addingIdentifiers').remove();
    }, this)
}

function removeAll(issue, flatArray){
    // split string into rows
    // a,b,c,d|e,f,g,h
    var a = flatArray.split("|");
    if (a.length > 100) {
      var r = window.confirm('There are many items in the table. This operation can take a while. Please be patient and do not stop script or cancel it now.');
        if (! (r == true)) {
          return;
        }
      // show a loading message that the identifiers are being resolved
      jQuery('#error_msg.topBar.errors').clone().addClass('loading').attr('id', 'removingIdentifiers')
      .html('<p>Removing identifiers from a bag, please wait.</p>')
      .appendTo(jQuery("#error_msg.topBar.errors").parent()).show();
    }

  // loading img
  jQuery('<div/>', {
    'class': 'loading'
  }).appendTo(jQuery("#sidebar ul li." + issue));

    jQuery.each(a, function(i, v) {
      // use a queue for long running code
      im.queue.put(function() {
        // split rows into vars
        var b = v.split(",");
        removeIdFromBag(b[0], b[1], b[2], b[3]);
      }, this);
    });

    // queue in switching of links and message
    im.queue.put(function() {
      jQuery("#sidebar ul li." + issue + ' div.loading').remove();
        toggleBagLinks(issue, 'remove');
        jQuery('#removingIdentifiers').remove();
    }, this);
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

    // update individual adders in the sidebar as well
    if (jQuery('#sidebar').length > 0) {
      jQuery('#sidebar ul li.' + issue).toggleClass('added');
    }
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
