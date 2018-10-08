<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- templateProblems.jsp -->

<html:xhtml/>

<c:if test="${problems != null}">
  <div class="body">
  <fmt:message key="templateProblems.header"/>
  <ul>
    <c:forEach items="${problems}" var="problem">
      <li>${problem}</li>
    </c:forEach>
  </ul>
</c:if>

<!-- /templateProblems.jsp -->
