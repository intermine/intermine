<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1" prefix="str" %>

<!-- widget.jsp -->
<tiles:importAttribute name="widget" ignore="false" />
<tiles:importAttribute name="bag" ignore="false" />

<html:xhtml/>
<c:set var="split" value="${fn:split(widget.class,'.')}"/>
<c:set var="type" value="${split[fn:length(split)-1]}"/>

<c:choose>
    <c:when test="${type == 'GraphWidgetConfig'}" >
        <c:set var="widgetStyle" value="chart"/>
    </c:when>
    <c:when test="${type == 'EnrichmentWidgetConfig'}" >
        <c:set var="widgetStyle" value="enrichment"/>
    </c:when>
    <c:when test="${type == 'TableWidgetConfig'}" >
        <c:set var="widgetStyle" value="table"/>
    </c:when>
</c:choose>

<div id="${widget.id}-widget" class="bootstrap widget"></div>
<tiles:insert template="widgetScript.jsp">
    <tiles:put name="token" value="${token}"/> 
    <tiles:put name="widgetId" value="${widget.id}"/> 
    <tiles:put name="bagName" value="${bag.name}"/> 
    <tiles:put name="style" value="${widgetStyle}"/>
</tiles:insert>

<!-- /widget.jsp -->
