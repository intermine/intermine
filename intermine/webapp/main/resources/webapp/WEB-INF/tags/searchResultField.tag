<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>

<%@ attribute name="field" required="true" type="org.intermine.web.search.KeywordSearchResult.ObjectField" %>
<%@ attribute name="nullValue" required="true" %>

<c:choose>
  <%-- print each field configured for this object --%>
  <c:when test="${!empty field.config && !empty field.config.displayer}">
    <%-- display fields that have custom displayers. --%>
    <c:set var="interMineObject" value="${searchResult.object}" scope="request" />
    <span class="value">
      <tiles:insert page="${field.config.displayer}">
        <tiles:put name="expr" value="${field.config.fieldExpr}" />
      </tiles:insert> </span>
    </span>
  </c:when>
  <%-- Display fields for which a particular field expression is configured. --%>
  <c:when test="${!empty field.value}">
    <span class="value">${field.value}</span>
  </c:when>
  <c:otherwise>
    <c:out value="${nullValue}"/>
  </c:otherwise>
</c:choose>
