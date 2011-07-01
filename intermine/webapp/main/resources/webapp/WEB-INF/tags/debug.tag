<%@ tag body-content="scriptless" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@ attribute name="message" required="true" %>

<c:if test="${WEB_PROPERTIES['webapp.debug']}">
    <!-- ${message} -->
</c:if>
