<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<table border="0" height="100%" width="100%" bordercolor="yellow">
    <tr>
        <td height="100%" width="15%" rowspan=2><tiles:get name="left"/></td>
        <td valign="top"><tiles:get name="top"/></td>
    </tr>
    <tr><td valign="bottom"><tiles:get name="bottom"/></td></tr>
</table>

