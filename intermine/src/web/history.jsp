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
    <html:form action="/createBoolean">
      <tiles:get name="historyBagView"/>
      <tiles:get name="historyQueryView"/>
      <%--
        <html:submit property="action">
          <fmt:message key="history.union"/>
        </html:submit>
        <html:submit property="action">
          <fmt:message key="history.intersect"/>
        </html:submit>
        --%>
    </html:form>
  </c:otherwise>
</c:choose>
<!-- /history.jsp -->
