<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>

<tiles:importAttribute scope="request"/>

<!-- objectDetailsPage.jsp -->
<c:set var="viewType" value="detail" scope="request"/>
<tiles:insert name="objectView.tile"/>
<br/>
<html:link action="/results">
  <fmt:message key="results.returnToResults"/>
</html:link>
<!-- /objectDetailsPage.jsp -->
