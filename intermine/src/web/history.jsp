<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>

<!-- history.jsp -->
<c:choose>
  <c:when test="${empty SAVED_BAGS && empty SAVED_QUERIES}">
    <fmt:message key="history.nohistory"/>
  </c:when>
  <c:otherwise>
    <tiles:get name="historyBagView"/>
    <br/>
    <tiles:get name="historyQueryView"/>
  </c:otherwise>
</c:choose>
<!-- /history.jsp -->
