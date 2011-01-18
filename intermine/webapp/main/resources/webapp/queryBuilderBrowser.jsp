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
    /*if (isExplorer()) {
      return true;
    }*/
    if ($(id).innerHTML=='') {
      new Ajax.Updater(id, '<html:rewrite action="/queryBuilderChange"/>',
        {parameters:'method=ajaxExpand&path='+path, asynchronous:true});
      $('img_'+path).src='images/minus.gif';
    } else {
      // still need to call queryBuilderChange
      $('img_'+path).src='images/plus.gif';
      path = path.substring(0, path.lastIndexOf('.'));
      new Ajax.Request('<html:rewrite action="/queryBuilderChange"/>',
        {parameters:'method=ajaxCollapse&path='+path, asynchronous:true});
      $(id).innerHTML='';
    }
    return false;
  }

  function addConstraint(path) {
    /*if (isExplorer()) {
      return true;
    }*/
    new Ajax.Updater('queryBuilderConstraint', '<html:rewrite action="/queryBuilderChange"/>',
      {parameters:'method=ajaxNewConstraint&path='+path, asynchronous:true, evalScripts:true,
      onSuccess: function() {
        new Ajax.Updater('query-build-summary', '<html:rewrite action="/queryBuilderChange"/>',
          {parameters:'method=ajaxRenderPaths', asynchronous:true, evalScripts:true, onSuccess: function() {
             new Boxy(jQuery('#constraint'), {title: "Constraint for " + path, modal:true, unloadOnHide: true})
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
