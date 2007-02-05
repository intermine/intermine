<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<tiles:importAttribute/>

<!-- main.jsp -->

<html:xhtml/>

<script type="text/javascript" src="js/queryBuilder.js" ></script>

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
    </td>
  </tr>
  
  <a name="constraint-editor"></a>
    <tr>
      <td valign="top">
        <div id="mainConstraint">
          <c:if test="${editingNode != null}">
            <tiles:insert name="mainConstraint.tile"/>
          </c:if>
        </div>
      </td>
    </tr>
</table>

<!-- /main.jsp -->
