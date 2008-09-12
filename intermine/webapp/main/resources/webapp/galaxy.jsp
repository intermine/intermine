<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<!-- galaxy.jsp -->

<div class="topBar messages">
${urlSendBack}
<form action="${GALAXY_URL}" name="galaxy_exchange" method="POST">
  <input type="hidden" name="URL" value="${urlSendBack}">
  Send results to GALAXY: <input type="submit" name="Send" value="Send">
</form>
</div>

<!-- /galaxy.jsp -->