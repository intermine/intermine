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
