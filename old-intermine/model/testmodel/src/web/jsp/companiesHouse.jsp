<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- companiesHouse.jsp -->
<%-- Display an icon linked to companys house --%>
<%-- WEB_PROPERTIES is a Map created from web.properties --%>
<html:link href="${WEB_PROPERTIES['companieshouse.url.prefix']}${object.name}">
  <html:img src="model/companiesHouse.png"/>
</html:link>
<!-- /companiesHouse.jsp -->
