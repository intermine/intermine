<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<tiles:importAttribute/>

<!-- main.jsp -->

<html:xhtml/>

<script type="text/javascript" src="js/queryBuilder.js" ></script>
<%-- Javascript files must be included there because they are not processed when mainConstraint 
 tile is added by Ajax.Updater --%>
<script type="text/javascript" src="js/autocompleter.js"></script>
<script type="text/javascript" src="js/tagSelect.js" ></script>

<link rel="stylesheet" href="css/autocompleter.css" type="text/css" />

<div id="mainBrowser" class="modelbrowse" ><tiles:insert page="/mainBrowser.jsp"/></div>

<div id="rightColumn" >
  <div id="main-paths">
    <tiles:insert name="mainPaths.tile"/>
  </div>
  <a name="constraint-editor"></a>
</div>

<div style="clear:both;">
  <tiles:get name="view.tile"/>
</div>

<div id="mainConstraint">
  <tiles:insert name="mainConstraint.tile"/>
</div>
<br clear="all"/>
<!-- /main.jsp -->
