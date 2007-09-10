<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>



<!-- mainBrowser.jsp -->

<html:xhtml/>

<script type="text/javascript">
  <!--
  function toggle(id, path) {
    if (isExplorer()) {
      return true;
    }
    if ($(id).innerHTML=='') {
      new Ajax.Updater(id, '<html:rewrite action="/mainChange"/>',
        {parameters:'method=ajaxExpand&path='+path, asynchronous:true});
      $('img_'+path).src='images/minus.gif';
    } else {
      // still need to call mainChange
      $('img_'+path).src='images/plus.gif';
      path = path.substring(0, path.lastIndexOf('.'));
      new Ajax.Request('<html:rewrite action="/mainChange"/>',
        {parameters:'method=ajaxCollapse&path='+path, asynchronous:true});
      $(id).innerHTML='';
    }
    return false;
  }
  
  function addConstraint(path) {
    if (isExplorer()) {
      return true;
    }
    new Ajax.Updater('mainConstraint', '<html:rewrite action="/mainChange"/>',
      {parameters:'method=ajaxNewConstraint&path='+path, asynchronous:true, evalScripts:true,
      onSuccess: function() {
        new Ajax.Updater('main-paths', '<html:rewrite action="/mainChange"/>',
          {parameters:'method=ajaxRenderPaths', asynchronous:true, evalScripts:true});
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
<div class="body">
  <div> 
    <fmt:message key="query.currentclass.detail"/>
  </div>
  <br/>
  <c:if test="${!empty navigation}">
    <c:forEach items="${navigation}" var="entry" varStatus="status">
      <fmt:message key="query.changePath" var="changePathTitle">
        <fmt:param value="${entry.key}"/>
      </fmt:message>
      <im:viewableSpan path="${entry.value}" viewPaths="${viewPaths}" idPrefix="nav">
        <html:link action="/mainChange?method=changePath&amp;prefix=${entry.value}&amp;path=${navigationPaths[entry.key]}"
                   title="${changePathTitle}">
          <c:out value="${entry.key}"/>
        </html:link>
      </im:viewableSpan>
      <c:if test="${!status.last}">&gt;</c:if>
    </c:forEach>
    <br/><br/>
  </c:if>
  <tiles:insert page="/mainBrowserLines.jsp"/>
</div>

<!-- /mainBrowser.jsp -->
