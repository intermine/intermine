<%@ tag body-content="empty" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<%@ attribute name="date" required="true" type="java.util.Date" %>
<%@ attribute name="type" type="java.lang.Boolean" %>

<span style="white-space: nowrap">
  <c:choose>
    <c:when test="${!empty type && type == 'short'}">
      <fmt:formatDate value="${date}" type="both" pattern="yyyy-MM-dd"/>
    </c:when>
    <c:otherwise>
      <fmt:formatDate value="${date}" type="both" pattern="yyyy-MM-dd HH:mm z"/>
    </c:otherwise>
  </c:choose>
</span>
