<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>

<tiles:importAttribute/>

<!-- results.jsp -->
<tiles:get name="table"/>
<tiles:get name="saveQuery"/>
<tiles:get name="queryName"/>
<html:link action="/query"><fmt:message key="results.returnToQuery"/></html:link>
<!-- /results.jsp -->
