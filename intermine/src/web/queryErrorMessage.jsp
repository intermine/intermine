<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>

<!-- queryErrorMessage.jsp -->
<c:if test="${constraintErrors != null}">
    <bean:message key="query.constrainterror"/>
</c:if>
<!-- /queryErrorMessage.jsp -->
