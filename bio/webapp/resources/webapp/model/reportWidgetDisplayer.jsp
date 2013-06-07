<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1" prefix="str" %>

<!-- reportWidgetDisplayer.jsp -->
<div id="report-widget-displayer-example" class="foundation"></div>
<script>
intermine.load('report-widgets', function(err) {
    var widgets = new intermine.reportWidgets('http://localhost:8080/hero/service');
    widgets.load('publications-displayer', '#report-widget-displayer-example', { 'symbol': 'zen' });
});
</script>
<!-- /reportWidgetDisplayer.jsp -->