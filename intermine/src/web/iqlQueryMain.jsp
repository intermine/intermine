<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<!-- fqlQueryMain.jsp -->
<table width="100%">
  <tr>
    <td valign="top" width="20%">
<tiles:get name="savedBagView"/>
<tiles:get name="savedQueryView"/>
    </td>
    <td valign="top" width="80%">
<tiles:get name="queryErrorMessage"/>
<tiles:get name="fqlQuery"/>
    </td>
  </tr>
</table>
<!-- /fqlQueryMain.jsp -->
