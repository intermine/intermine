<%@ tag body-content="empty" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<%@ attribute name="date" required="true" type="java.util.Date" %>
<%@ attribute name="type" type="java.lang.String" %>

<span style="white-space: nowrap">
  <c:choose>
    <c:when test="${!empty type}">
      <c:choose>
      <c:when test="${type == 'longDate'}">
            <fmt:formatDate value="${date}" type="date" dateStyle="long" />
      </c:when>
      <c:otherwise>
         <fmt:formatDate value="${date}" type="both" pattern="yyyy-MM-dd"/>
      </c:otherwise>
      </c:choose>
    </c:when>
    <c:otherwise>
      <fmt:formatDate value="${date}" type="both" pattern="yyyy-MM-dd HH:mm z"/>
    </c:otherwise>
  </c:choose>
</span>
