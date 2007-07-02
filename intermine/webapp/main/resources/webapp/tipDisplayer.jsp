<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<link rel="stylesheet" type="text/css" href="tips/css/tips.css"/>

<!-- tipDisplayer.jsp -->
<div class="body">
		<jsp:include page="tips/tip${id}_long.jsp"/>
		<html:link action="/tips?n=${tipcount}">Show all tips &gt;&gt;</html:link>
</div>
<!-- /tipDisplayer.jsp -->