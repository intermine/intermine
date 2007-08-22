<%@ tag body-content="scriptless"  %>
<%@ attribute name="id" required="false" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>


<div class="body">
    <c:if test="${empty id || !COLLAPSED[id]}">
      <jsp:doBody/>
    </c:if>
</div>
