<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- templateProblems.jsp -->

<html:xhtml/>

<c:if test="${param.type == 'user'}">
  <c:set var="template" value="${PROFILE.savedTemplates[param.name]}"/>
</c:if>
<c:if test="${param.type == 'global'}">
  <c:set var="template" value="${GLOBAL_TEMPLATE_QUERIES[param.name]}"/>
</c:if>
<c:if test="${param.type == 'saved'}">
  <c:set var="template" value="${PROFILE.savedQueries[param.name].pathQuery}"/>
</c:if>


<c:if test="${template != null}">
  <div class="body">
  <fmt:message key="templateProblems.header"/>
  <ul>
    <c:forEach items="${template.problems}" var="problem">
      <li>${problem.message}</li>
    </c:forEach>
  </ul>
</c:if>

<!-- /templateProblems.jsp -->
