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
<c:set var="bagName" value="${bag.name}"/>
<c:set var="widgetId" value="${widget.id}"/>

<c:choose>
	<c:when test="${type == 'GraphWidgetConfig'}" >
		<div id="${widgetId}-widget" class="widget"></div>
		<script type="text/javascript">(function() { widgets.chart("${widgetId}", "${bagName}", "#${widgetId}-widget"); })();</script>
	</c:when>
	<c:when test="${type == 'EnrichmentWidgetConfig'}" >
		<div id="${widgetId}-widget" class="widget"></div>
		<script type="text/javascript">(function() { widgets.enrichment("${widgetId}", "${bagName}", "#${widgetId}-widget"); })();</script>
	</c:when>
</c:choose>
<!-- /widget.jsp -->