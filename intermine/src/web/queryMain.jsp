<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<!-- queryMain.jsp -->
<table width="100%">
  <tr>
    <td valign="top" width="20%">
<tiles:get name="queryClassSelect"/>
<tiles:get name="savedBagView"/>
<tiles:get name="savedQueryView"/>
    </td>
    <td valign="top" width="80%">
<tiles:get name="queryErrorMessage"/>
<tiles:get name="queryBuild"/>
<tiles:get name="loadQuery"/>
    </td>
  </tr>
  <tr>
    <td colspan="2">
      <hr/>
    </td>
  </tr>
  <tr>
    <td>
<tiles:get name="restartQuery"/>
    </td>
  </tr>
</table>
<!-- /queryMain.jsp -->
