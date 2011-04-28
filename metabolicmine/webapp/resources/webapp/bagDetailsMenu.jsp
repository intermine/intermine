<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil"%>

<%-- jQuery.scrollTo relies on inlinetemplate.js! --%>

<div class="wrap">
  <span>Content:</span>
  <div class="menu-wrap">
    <div class="links">
      <html:link action="/bagDetails.do?name=${bagNameURLized}#list"
      onclick="jQuery('a[name=list]').scrollTo('slow', 'swing', -20);return false;"
      >List</html:link>

      <html:link action="/bagDetails.do?name=${bagNameURLized}#widgets"
      onclick="jQuery('a[name=widgets]').scrollTo('slow', 'swing', -20);return false;"
      >Widgets</html:link>

      <html:link action="/bagDetails.do?name=${bagNameURLized}#templates"
      onclick="jQuery('a[name=templates]').scrollTo('slow', 'swing', -20);return false;"
      >Templates</html:link>
    </div>
  </div>
  <div class="clear">&nbsp;</div>
</div>