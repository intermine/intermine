<%@ tag body-content="empty" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<%@ attribute name="date" required="true" type="java.util.Date" %>
<%@ attribute name="type" type="java.lang.String" %>

<%-- The long date value will be parsed by javascript (see intermine.js)
into a locale-appropriate string on the client --%>
<span class="intermine timestamp">${date.time}</span>
