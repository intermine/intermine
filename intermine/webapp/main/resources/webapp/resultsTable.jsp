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
<tiles:importAttribute name="bagName" ignore="true"/>
<tiles:importAttribute name="cssClass" ignore="true"/>
<tiles:importAttribute name="pageSize" ignore="true"/>
<tiles:importAttribute name="query" ignore="true"/>
<tiles:importAttribute name="consumerContainer" ignore="true"/>
<tiles:importAttribute name="consumerBtnClass" ignore="true"/>
<tiles:importAttribute name="successCallBack" ignore="true"/>
<tiles:importAttribute name="tableIsOpen" ignore="true"/>

<c:if test="${empty query}">
    <c:set var="query" value="${QUERY}"/>
</c:if>

<c:if test="${empty pageSize}">
    <c:set var="pageSize" value="25"/>
</c:if>

<c:if test="${empty tableIsOpen}">
    <c:set var="tableIsOpen" value="true"/>
</c:if>

<c:set var="initValue" value="0"/>

<c:if test="${empty currentUniqueId}">
    <c:set var="currentUniqueId" value="${initValue}" scope="application"/>
</c:if>

<c:set var="tableContainerId" value="_unique_id_${currentUniqueId}" scope="request"/>

<c:set var="currentUniqueId" value="${currentUniqueId + 1}" scope="application"/>

<c:if test="${! empty query.title }">
  <%-- We want to ignore path-queries. This is horrific. I know that --%>
  <c:catch var="exception">
    <c:set var="templateName" value="${ query.name }"/>
  </c:catch>
  <c:if test="${empty exception}">
    <tiles:insert template="templateTitle.jsp">
        <tiles:put name="templateQuery" beanName="query"/>
        <tiles:put name="clickable" value="true"/>
    </tiles:insert>
  </c:if>
</c:if>

<c:choose>
    <c:when test="${not empty query.json}">
        <c:set var="queryJson" value="${query.json}"/>
    </c:when>
    <c:otherwise>
        <c:set var="queryJson" value="{}"/>
    </c:otherwise>
</c:choose>

<div id="${tableContainerId}" class="${cssClass}">
    <c:if test="${!tableIsOpen}">
        <button class="open-table btn btn-default ${cssClass}">
            <fmt:message key="results.show.details"/>
        </button>
    </c:if>
</div>

<script type="text/javascript">
jQuery(function() {
    var customGalaxy = "${GALAXY_URL}";
    var url = window.location.origin + "/${WEB_PROPERTIES['webapp.path']}/loadQuery.do";
    if (customGalaxy !== "") {
        imtables.configure('Download.Galaxy.Current', customGalaxy);
    }
    var consumers = null, consumerBtnClass = null;
    <c:if test="${!empty consumerContainer}">
    consumers = document.querySelector('${consumerContainer}');
    </c:if>
    <c:if test="${!empty consumerBtnClass}">
    consumerBtnClass = '${consumerBtnClass}';
    </c:if>

    <c:if test="${tableIsOpen}">
    openTable();
    </c:if>
    <c:if test="${!tableIsOpen}">
    jQuery('#${tableContainerId} > .open-table').click(function () {
        jQuery(this).remove();
        openTable();
    });
    </c:if>

    function openTable () {
        imtables.loadDash(
            '#${tableContainerId}',
            {size: ${pageSize}, consumerContainer: consumers, consumerBtnClass: consumerBtnClass},
            {service: $SERVICE, query: ${queryJson}}
        ).then(
            withTable,
            FailureNotification.notify
        );
    }

    function withTable (table) {
        table.history.on('changed:current', updateTrail);
        table.bus.on('list-action:failure', LIST_EVENTS['failure']);
        table.bus.on('list-action:success', LIST_EVENTS['success']);
        <c:if test="${!empty successCallBack}">
        ${successCallBack}(table);
        </c:if>
        
        function updateTrail () {
            var query = table.history.getCurrentQuery()
            var xml = query.toXML();
            var $trail = jQuery('.objectTrailLinkResults');
            $trail.attr({href: url + '?method=xml&query=' + escape(xml)});
        }
    }
});
</script>


