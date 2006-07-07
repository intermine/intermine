<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- bag.jsp -->
<html:xhtml/>

<div class="body">
<table id="bagTable">
  <tr>
    <td><c:import url="bagView.jsp"/></td>
    <td><c:import url="bagBuild.jsp"/></td>
  </tr>
</table>
</div>
<!-- /bag.jsp -->