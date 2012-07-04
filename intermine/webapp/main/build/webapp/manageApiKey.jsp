<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- managerApiKey.jsp -->
<html:xhtml/>
&nbsp;

<h2><fmt:message key="apikey.heading"/></h2>

<p class="apikey"><fmt:message key="apikey.description"/></p>

<div class="apikey">
<c:choose>
    <c:when test="${empty PROFILE.apiKey}">
        <span class="apikey nokey">
            <fmt:message key="apikey.nokey"/>
        </span>
    </c:when>
    <c:otherwise>
        <span class="apikey">
            <c:out value="${PROFILE.apiKey}"/>
        </span>
    </c:otherwise>
</c:choose>
</div>

<button id="newApiKeyButton">
    <fmt:message key="apikey.generate"/>
</button>

<c:if test="${empty PROFILE.apiKey}">
  <c:set var="delete_css" value="display: none;"/>
</c:if>

<button id="deleteApiKeyButton" style="${delete_css}">
  <fmt:message key="apikey.deleteKey"/>
</button>

<script type="text/javascript">
$CURRENT_USER="${PROFILE.username}";
</script>

<!-- /managerApiKey.jsp -->
