<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<!-- queryMain.jsp -->
<table border="0" cellpadding="2" bordercolor="blue" height="100%">
    <tr><td align="top" height="15%"><tiles:get name="querySelectBuild"/></td></tr>
    <tr><td align="top" height="15%"><tiles:get name="aliasChange"/></td></tr>
    <tr><td align="top" height="15%"><tiles:get name="runQuery"/></td></tr>
    <tr><td align="top" height="15%"><tiles:get name="queryView"/></td></tr>
    <tr><td align="top" height="15%"><tiles:get name="loadQuery"/></td></tr>  
    <tr><td align="top" height="15%"><tiles:get name="restartQuery"/></td></tr>  
</table>
<!-- /queryMain.jsp -->
