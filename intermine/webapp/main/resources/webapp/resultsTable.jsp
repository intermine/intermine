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

<link type="text/css" rel="stylesheet" href="${jsLib}/lib/css/cupertino/jquery-ui-1.8.18.custom.css"></link>
<link type="text/css" rel="stylesheet" href="${jsLib}/lib/google-code-prettify/prettify.css"></link>

<script src="${jsLib}/lib/underscore-min.js"></script>
<script src="${jsLib}/lib/backbone.js"></script>
<script src="${jsLib}/lib/jquery-ui-1.8.18.custom.min.js"></script>
<script src="${jsLib}/lib/DataTables-1.9.0/media/js/jquery.dataTables.js"></script>
<script src="${jsLib}/lib/paging.js"></script>
<script src="${jsLib}/lib/google-code-prettify/prettify.js"></script>
<script src="${jsLib}/lib/bootstrap-tab.js"></script>
<script src="${jsLib}/lib/bootstrap-button.js"></script>
<script src="${jsLib}/lib/bootstrap-typeahead.js"></script>
<script src="${jsLib}/lib/bootstrap-tooltip.js"></script>
<script src="${jsLib}/lib/bootstrap-dropdown.js"></script>
<script src="${jsLib}/lib/bootstrap-modal.js"></script>
<script src="${jsLib}/lib/imjs/src/model.js"></script>
<script src="${jsLib}/lib/imjs/src/lists.js"></script>
<script src="${jsLib}/lib/imjs/src/service.js"></script>
<script src="${jsLib}/lib/imjs/src/query.js"></script>
<script src="${jsLib}/js/imtables.js"></script>

<script>
(function() {
    var query = ${QUERY.json};
    console.log(query);
    var service = new intermine.Service({
        "root": "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}",
        "token": "${PROFILE.dayToken}"
    });

    jQuery(function() {
        var view = new intermine.query.results.CompactView(service, query);
        view.$el.appendTo('#table-container');
        view.render();
    });
})();
</script>

<div id="table-container" class="bootstrap"></div>

