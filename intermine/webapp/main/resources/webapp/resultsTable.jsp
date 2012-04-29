<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>

<html:xhtml/>

<c:set var="jsLib" value="${WEB_PROPERTIES['ws.imtables.provider']}"/>
<%-- Required for displaying the contents of invalid bags --%>
<tiles:importAttribute name="invalid" ignore="true"/>
<tiles:importAttribute name="bag" ignore="true"/>
<tiles:importAttribute name="cssClass" ignore="true"/>

<link type="text/css" rel="stylesheet" href="${jsLib}/css/bootstrap.css"></link>
<link type="text/css" rel="stylesheet" href="${jsLib}/lib/css/flick/jquery-ui-1.8.19.custom.css"></link>
<link type="text/css" rel="stylesheet" href="${jsLib}/lib/google-code-prettify/prettify.css"></link>
<link type="text/css" rel="stylesheet" href="${jsLib}/css/tables.css"></link>
<link type="text/css" rel="stylesheet" href="${jsLib}/css/flymine.css"></link>

<script src="${jsLib}/lib/underscore-min.js"></script>
<script src="${jsLib}/lib/backbone.js"></script>

<script src="js/im.js"></script>

<script src="${jsLib}/js/deps.js"></script>
<script src="${jsLib}/js/imtables.js"></script>

<c:set var="initValue" value="0"/>

<c:if test="${empty currentUniqueId}">
    <c:set var="currentUniqueId" value="${initValue}" scope="application"/>
</c:if>

<c:set var="tableContainerId" value="_unique_id_${currentUniqueId}" scope="request"/>

<c:set var="currentUniqueId" value="${currentUniqueId + 1}" scope="application"/>

<script>
(function() {
    intermine.css.headerIcon = "fm-header-icon";
    var query = ${QUERY.json};
    var service = new intermine.Service({
        "root": "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}",
        "token": "${PROFILE.dayToken}"
    });

    jQuery(function() {
        var view = new intermine.query.results.CompactView(service, query);
        view.$el.appendTo('#${tableContainerId}');
        view.render();
    });
})();
</script>

<div id="${tableContainerId}" class="${cssClass}"></div>
