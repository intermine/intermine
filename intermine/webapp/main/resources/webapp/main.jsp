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
<script type="text/javascript" src="js/imdwr.js" ></script>
<script type="text/javascript" src="js/tagSelect.js" ></script>
<link rel="stylesheet" href="css/autocompleter.css" type="text/css" />

<table class="query" width="100%" cellspacing="0">
  <tr>
    <td rowspan="2" valign="top" width="50%" class="modelbrowse">
      <tiles:insert page="/mainBrowser.jsp"/>
    </td>

    <td valign="top">
      <div id="main-paths">
        <tiles:insert name="mainPaths.tile"/>
      </div>
      <tiles:insert page="/mainLogic.jsp"/>
      	<div id="mainConstraint">
          <c:if test="${editingNode != null}">
            <tiles:insert name="mainConstraint.tile"/>
          </c:if>
        </div>
      </td>
    </tr>
</table>

<!-- /main.jsp -->
