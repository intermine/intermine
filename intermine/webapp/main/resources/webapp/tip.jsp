<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!-- tip.jsp -->
<p>
<table>
<tr>
	<td><html:link action="/tip?id=${randomTip}"><img src="<html:rewrite page="/tips/images/tip${randomTip}.png" />" border="0" height="16" width="16" /></html:link></td>
	<td class="body"><jsp:include page="tips/tip${randomTip}_short.jsp"/></td>
</tr>
</table>
</p>
<p><html:link action="/tip?id=${randomTip}" target="_top">Read more &gt;&gt;</html:link></p>

<!-- /tip.jsp -->
