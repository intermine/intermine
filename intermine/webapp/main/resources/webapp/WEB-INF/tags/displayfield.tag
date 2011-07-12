<%@ tag body-content="scriptless" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>

<%@ attribute name="path" required="true" %>

<c:out value="${imf:formatFieldStr(path, INTERMINE_API, WEBCONFIG)}"/>

