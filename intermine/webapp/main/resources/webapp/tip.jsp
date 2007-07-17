<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!-- tip.jsp -->
	<h3>Did you know?</h3>
	<html:link action="/tip?id=${randomTip}" target="_top"><img src="<html:rewrite page="/tips/images/tip${randomTip}.png" />" border="0" height="16" width="16" /></html:link>&nbsp;&nbsp;
	<jsp:include page="tips/tip${randomTip}_short.jsp"/>&nbsp;<html:link action="/tip?id=${randomTip}" target="_top">Read more ...</html:link>

<!-- /tip.jsp -->
