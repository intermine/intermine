<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>



<!-- queryBuilderBrowser.jsp -->

<html:xhtml/>
<script type="text/javascript">
  <!--
  function toggleNode(id, path) {
    if ($(id).innerHTML=='') {
        <%-- can't touch these --%>
        var parents = jQuery("#" + id.replace(/\./g, "\\.")).parents("*");
        var cantTouchThese = new Array();
        for (var i = 0; i < parents.length; i++) {
          var parentId = jQuery(parents[i]).attr('id');
          if (typeof parentId !== 'undefined' && parentId !== false && parentId.length > 0) {
            cantTouchThese.push(parentId);
          }
        }

        <%-- collapse all expanded --%>
        jQuery("div.browserline img.toggle").each(function() {
            if (jQuery(this).attr("src").indexOf("minus") != -1) {
                var id = jQuery(this).parent().attr('title');
                var target = id.replace(/\./g, "\\.");
                <%-- clear the target div if not our new toggle's daddy --%>
                if (jQuery("#" + target).exists() && cantTouchThese.indexOf(id) < 0) {
                  jQuery("#" + target).html('');
                  jQuery(this).attr('src', 'images/plus.gif');
                }
            }
        });

      new Ajax.Updater(id, '<html:rewrite action="/queryBuilderChange"/>',
        {parameters:'method=ajaxExpand&path='+path, asynchronous:true});
      $('img_'+path).src='images/minus.gif';

      <%-- TODO: scroll to target --%>

    } else {
      <%-- collapsing --%>
      jQuery.get('<html:rewrite action="/queryBuilderChange"/>' + '?method=ajaxCollapse&path=' +
    		  ((path.split(".").length > 2) ? path.substring(0, path.lastIndexOf('.')) : path),
    	  function(r) {
	          jQuery('#img_' + path.replace(/\./g, "\\.")).attr('src', 'images/plus.gif');
	          jQuery('#' + id.replace(/\./g, "\\.")).remove();
    	  }
      );
    }

    return false;
  }

  function addConstraint(path, displayPath) {
    /*if (isExplorer()) {
      return true;
    }*/
    displayPath = displayPath || path;
    new Ajax.Updater('queryBuilderConstraint', '<html:rewrite action="/queryBuilderChange"/>',
      {parameters:'method=ajaxNewConstraint&path='+path, asynchronous:true, evalScripts:true,
      onSuccess: function() {
        new Ajax.Updater('query-build-summary', '<html:rewrite action="/queryBuilderChange"/>',
          {parameters:'method=ajaxRenderPaths', asynchronous:true, evalScripts:true, onSuccess: function() {
             new Boxy(jQuery('#constraint'), {title: "Constraint for " + displayPath, modal:true, unloadOnHide: true})
          }
        });
      }
    });
    return false;
  }

  function isExplorer() {
    return (navigator.appVersion.toLowerCase().indexOf('msie') >= 0);
  }

  //-->
</script>

  <div class="heading">
    <fmt:message key="query.currentclass"/>
  </div>
  <div class="body" id="browserbody">
    <div>
      <img class="icon" src="images/icons/queries-64.png" alt="query builder icon" />
      <fmt:message key="query.currentclass.detail"/>
    </div>
    <br/>
    <tiles:insert page="/queryBuilderBrowserLines.jsp"/>
  </div>

<!-- /queryBuilderBrowser.jsp -->
