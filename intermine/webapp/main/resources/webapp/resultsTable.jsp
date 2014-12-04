<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>

<html:xhtml/>

<tiles:importAttribute name="invalid" ignore="true"/>
<tiles:importAttribute name="bag" ignore="true"/>
<tiles:importAttribute name="cssClass" ignore="true"/>
<tiles:importAttribute name="pageSize" ignore="true"/>
<tiles:importAttribute name="query" ignore="true"/>

<c:if test="${empty query}">
    <c:set var="query" value="${QUERY}"/>
</c:if>

<c:if test="${empty pageSize}">
    <c:set var="pageSize" value="25"/>
</c:if>
  

<c:set var="initValue" value="0"/>

<c:if test="${empty currentUniqueId}">
    <c:set var="currentUniqueId" value="${initValue}" scope="application"/>
</c:if>

<c:set var="tableContainerId" value="_unique_id_${currentUniqueId}" scope="request"/>

<c:set var="currentUniqueId" value="${currentUniqueId + 1}" scope="application"/>

<c:if test="${! empty query.title}">
    <tiles:insert template="templateTitle.jsp">
        <tiles:put name="templateQuery" beanName="query"/>
        <tiles:put name="clickable" value="true"/>
    </tiles:insert>
</c:if>

<c:choose>
    <c:when test="${not empty query.json}">
        <c:set var="queryJson" value="${query.json}"/>
    </c:when>
    <c:otherwise>
        <c:set var="queryJson" value="{}"/>
    </c:otherwise>
</c:choose>

<div id="${tableContainerId}" class="${cssClass}"></div>

<script type="text/javascript">
jQuery(function() {
    intermine.css.headerIcon = "fm-header-icon";
    var customGalaxy = "${GALAXY_URL}";
    if (customGalaxy) intermine.options.GalaxyCurrent = customGalaxy;
    var opts = {
        type: 'table',
        service: $SERVICE,
        error: FailureNotification.notify,
        query: ${queryJson},
        events: LIST_EVENTS,
        properties: { pageSize: ${pageSize} }
    };
    var widget = jQuery('#${tableContainerId}').imWidget(opts);
    var url = window.location.protocol + "//" + window.location.host + "/${WEB_PROPERTIES['webapp.path']}/loadQuery.do";
    widget.states.on('revert add', function () {
        var query = widget.states.currentQuery;
        var xml = query.toXML();
        var $trail = jQuery('.objectTrailLinkResults');
        $trail.attr({href: url + '?method=xml&query=' + escape(xml)});
    });
});
</script>


