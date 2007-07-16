<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- bag.jsp -->
<html:xhtml/>

<div class="body">
<table padding="0px" margin="0px">
  <tr>

<%-- INSERT HELP TILE HERE - in a <td></td> --%> 

    <td valign="top" width="30%"><c:import url="bagBuild.jsp"/></td>
    <td valign="top" width="70%"><c:import url="bagView.jsp"/></td>
  </tr>
</table>
</div>
<!-- /bag.jsp -->
