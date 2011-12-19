<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<!-- queryBuilderBrowser.jsp -->

<html:xhtml/>
<script type="text/javascript">
  function toggleNode(id, path) {
	var image = jQuery("#img_" + path.replace(/\./g, "\\.")),
		qbURL = '<html:rewrite action="/queryBuilderChange"/>',
		id    = id.replace(/\./g, "\\.");
    
	if (image.attr('src') == 'images/plus.gif') {
      <%-- expanding --%>
      jQuery.get(qbURL + '?method=ajaxExpand&path=' + (path),
    	  function(data) {
	          image.attr('src', 'images/minus.gif');
	          jQuery("div#" + id).after(data);
    	  }
      );
    } else {
      <%-- collapsing --%>
      jQuery.get(qbURL + '?method=ajaxCollapse&path=' +
    		  ((path.split(".").length > 2) ? path.substring(0, path.lastIndexOf('.')) : path),
    	  function() {
	          image.attr('src', 'images/plus.gif');
	          jQuery('[id^="' + id + '\\."]').remove();
    	  }
      );
    }

    return false;
  }

  function addConstraint(path, displayPath) {
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
  
  <div class="body toolbar">
  	<input id="emptyFieldsCheckbox" type="checkbox" /> Show empty fields
  </div>

<script type="text/javascript">
	jQuery("input#emptyFieldsCheckbox").click(function() {
		//if (jQuery(this).is(':checked')) {
			jQuery('#queryBuilderBrowser #browserbody div.browserline.empty').toggle();
		//}
	});
</script>

<!-- /queryBuilderBrowser.jsp -->