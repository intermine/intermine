<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<!-- deprecated; moved to galaxyExportOptions.jsp -->
<!-- galaxy.jsp -->

<script type="text/javascript">
  jQuery(document).ready(function() {
    if ("${urlSendBack}" != "") {
      jQuery("#data_type").val("${dataType}");
      jQuery("#db").val("${genomeBuild}");
      jQuery("#info").val("${info}");
    }
  });
</script>

<c:if test="${!empty urlSendBack}">
    <form id="galaxyform" action="${GALAXY_URL}" name="galaxyform" method="POST">
    <input id="URL" type="hidden" name="URL" value="${urlSendBack}">
    <input id="data_type" type="hidden" name="data_type">
    <input id="db" type="hidden" name="db">
    <input id="info" type="hidden" name="info">
    <input id="organism" type="hidden" name="organism">
    <input id="description" type="hidden" name="description">
    </form>
</c:if>

<!-- /galaxy.jsp -->
