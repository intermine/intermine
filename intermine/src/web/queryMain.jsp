<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<!-- queryMain.jsp -->
<table width="100%">
  <tr>
    <td valign="top">
<tiles:get name="queryClassSelect"/>
<tiles:get name="savedBagView"/>
<tiles:get name="savedQueryView"/>
    </td>
    <td valign="top" width="100%">
<tiles:get name="queryErrorMessage"/>
<tiles:get name="queryBuild"/>
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
