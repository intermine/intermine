<%@ tag body-content="empty" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<%@ attribute name="date" required="true" type="java.util.Date" %>

<span style="white-space: nowrap">
  <fmt:formatDate value="${date}" type="both" pattern="yyyy-MM-dd K:mm a"/>
</span>
