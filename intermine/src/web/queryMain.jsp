<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<!-- queryMain.jsp -->
<table>
  <tr>
    <td valign="top">
<tiles:get name="queryClassSelect"/>
<tiles:get name="savedBagView"/>
<tiles:get name="savedQueryView"/>
    </td>
    <td valign="top">
<tiles:get name="queryErrorMessage"/>
<tiles:get name="queryBuild"/>
<tiles:get name="loadQuery"/>
    </td>
  </tr>
</table>
<!-- /queryMain.jsp -->
