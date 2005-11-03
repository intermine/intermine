<%@ tag body-content="scriptless"  %>
<%@ attribute name="id" required="false" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>


<div class="body">
  <c:choose>
    <c:when test="${empty id || !COLLAPSED[id]}">
      <jsp:doBody/>
    </c:when>
    <c:otherwise>
      <div class="collapsed"><fmt:message key="tag.body.hidden"/></div>
    </c:otherwise>
  </c:choose>
</div>
