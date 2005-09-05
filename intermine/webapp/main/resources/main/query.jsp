<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- query.jsp -->
<im:viewablejs idPrefixes="nav,browser,query,showing"/>
<tiles:insert page="templateSettings.jsp"/>
<tiles:get name="main"/>
<br/>
<tiles:get name="view"/>
<!-- /query.jsp -->
