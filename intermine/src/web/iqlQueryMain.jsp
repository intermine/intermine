<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<!-- iqlQueryMain.jsp -->
<table width="100%">
  <tr>
    <td valign="top" width="20%">
<tiles:get name="savedBagView"/>
<tiles:get name="savedQueryView"/>
    </td>
    <td valign="top" width="80%">
<tiles:get name="queryErrorMessage"/>
<tiles:get name="iqlQuery"/>
    </td>
  </tr>
</table>
<!-- /iqlQueryMain.jsp -->
