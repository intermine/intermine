<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<!-- bag.jsp -->
<html:xhtml/>

<div class="body">
<table padding="0px" margin="0px">
  <tr>
    <td valign="top" width="30%"><tiles:insert name="bagBuild.tile"/></td>
    <td valign="top" width="70%"><c:import url="bagView.jsp"/></td>
  </tr>
</table>
</div>
<!-- /bag.jsp -->
