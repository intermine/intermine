<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!-- aspectTemplates.jsp -->

<tiles:importAttribute name="aspectQueries"/>
<tiles:importAttribute name="aspectTitle"/>

        <ul>
        <c:forEach var="entry" items="${aspectQueries}">
            <c:set var="aspect" value="${entry.key}"/>
            <c:set var="templates" value="${entry.value}"/>
            <c:if test="${aspect == aspectTitle}">
                <c:forEach var="template" items="${templates}">
                   <c:set var="templateTitle" value="${fn:replace(template.title,'-->','&nbsp;<img src=\"images/icons/green-arrow-16.png\" style=\"vertical-align:bottom\">&nbsp;')}" />
                   <li><a href="template.do?name=${template.name}">${templateTitle}</a></li>
                </c:forEach>
            </c:if>
        </c:forEach>
        </ul>
        <p class="more"><a href="templates.do?filter=${aspectTitle}">More queries</a></p>
<!-- /aspectTemplates.jsp -->
