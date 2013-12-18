<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<%-- Import all attributes --%>
<tiles:importAttribute/>

<c:if test="${empty templateQuery}">
    <c:set var="templateQuery" value="${QUERY}"/>
</c:if>

<html:xhtml/>

<!-- templateTitle.jsp -->
<h2 class="templateTitle">
    <html:link action="/template?name=${templateQuery.name}">
        <c:out value="${fn:replace(templateQuery.title,'-->','&nbsp;<img src=\"images/icons/green-arrow-24.png\" style=\"vertical-align:middle\">&nbsp;')}" escapeXml="false"/>
    </html:link>
    <tiles:insert name="setFavourite.tile">
        <tiles:put name="name" value="${templateQuery.name}"/>
        <tiles:put name="type" value="template"/>
    </tiles:insert>
</h2>

<%-- description --%>
<div class="templateDescription">${templateQuery.description}</div>

<!-- /templateTitle.jsp -->
