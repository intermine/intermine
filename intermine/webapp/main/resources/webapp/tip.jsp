<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<!-- tip.jsp -->
<div class="body">
	<jsp:include page="tips/tip${randomTip}_short.jsp"/>
	<br/>
	<html:img src="tips/images/tip${randomTip}a.png" width="400" height="130" />
	<br/>
	<html:link action="/tip?id=${randomTip}">Read more >>></html:link>
</div>
<!-- /tip.jsp -->