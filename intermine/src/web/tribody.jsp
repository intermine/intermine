<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<table border="0">
    <tr>
        <td rowspan=2><tiles:get name="left"/></td>
        <td><tiles:get name="top"/></td>
    </tr>
    <tr><td><tiles:get name="bottom"/></td></tr>
</table>

