<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<tiles:importAttribute/>

<!-- queryBuilder.jsp -->

<html:xhtml/>

<script type="text/javascript" src="js/queryBuilder.js" ></script>
<%-- Javascript files must be included there because they are not processed when queryBuilderConstraint
 tile is added by Ajax.Updater --%>
<script type="text/javascript" src="js/autocompleter.js"></script>
<script type="text/javascript" src="js/tagSelect.js" ></script>

<link rel="stylesheet" href="css/autocompleter.css" type="text/css" />

<script type="text/javascript">
  /**
   * jQuery function extension performing an immediate 'move/scroll to target'
   */
  jQuery.fn.extend({
    /**
     * @scrollIn A jQuery selector that will be scrolled, a (grand-)parent of 'this' !
     * @speed A string or number determining how long the animation will run
     * @easing A string indicating which easing function to use for the transition (linear or swing).
     * @val Extra offset in px
     * @onComplete A function to call once the animation is complete
     */
      moveInTo : function(scrollIn, speed, easing, val, onComplete) {
          return this.each(function() {
              var targetOffset = jQuery(this).offset().top - jQuery(scrollIn).offset().top + val;
              jQuery(scrollIn).animate({
                  scrollTop: targetOffset
              }, speed, easing, onComplete);
          });
      }
  });

  /**
   * Parse a custom anchor element and move to the desired elemenent
   */
  jQuery(document).ready(function() {
    // get URL
      var url = window.location.toString();
      // fetch anchor
      var start = url.indexOf("#anchor=");
      if (start > 0) {
        // strip identifier
        var anchor = url.substring(start + 8);
        // get an anchor link
        var link = jQuery('a').filter('[name$="'+anchor+'"]');
        link.show();
        // move there
        link.moveInTo("#browserbody", 0, 'swing', -20);
      }
  });
</script>

<div id="queryBuilderBrowser" class="modelbrowse" ><tiles:insert page="/queryBuilderBrowser.jsp"/></div>

<div id="rightColumn" >
  <div id="query-builder-summary">
    <tiles:insert name="queryBuilderSummary.tile"/>
  </div>
  <a name="constraint-editor"></a>
</div>
<div style="clear:both;">
  <tiles:get name="queryBuilderView.tile"/>
</div>
<div id="queryBuilderConstraint">
  <tiles:insert name="queryBuilderConstraint.tile"/>
</div>
<br clear="all"/>
<!-- /queryBuilder.jsp -->
