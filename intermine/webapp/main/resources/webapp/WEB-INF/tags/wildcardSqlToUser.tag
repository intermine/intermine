<%@ tag body-content="empty" %>
<%@ attribute name="sqlValue" required="true" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<%= org.intermine.web.logic.WebUtil.wildcardSqlToUser((String) jspContext.getAttribute("sqlValue")) %>
