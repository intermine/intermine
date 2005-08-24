<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<!-- history.jsp -->
<div class="body">
  <fmt:message key="history.intro"/>
</div>
<br/>
<c:choose>
  <c:when test="${empty PROFILE.savedBags && empty PROFILE.savedQueries && empty PROFILE.history}">
    <div class="body altmessage">
      <fmt:message key="history.nohistory"/>
    </div>
  </c:when>
  <c:otherwise>
    <tiles:get name="historyBagView"/>
    <tiles:insert name="historyQueryView">
      <tiles:put name="type" value="saved"/>
    </tiles:insert>
    <tiles:insert name="historyQueryView">
      <tiles:put name="type" value="history"/>
    </tiles:insert>
  </c:otherwise>
</c:choose>
<!-- /history.jsp -->
