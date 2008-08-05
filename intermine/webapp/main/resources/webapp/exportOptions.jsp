<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil" %>

<!-- exportOptions.jsp -->
<html:xhtml/>

<link rel="stylesheet" href="css/exportOptions.css" type="text/css" />

<c:choose>
  <c:when test="${type == 'csv' || type == 'excel'}">
    <fmt:message key="exporter.${type}.description">
      <fmt:param value="${WEB_PROPERTIES['max.excel.export.size']}"/>
    </fmt:message>
  </c:when>
  <c:otherwise>
    <fmt:message bundle="model" key="exporter.${type}.description"/>
  </c:otherwise>
</c:choose>
<br/>
<br/>

<html:form action="/${type}ExportAction">
  <c:choose>
    <c:when test="${type == 'csv'}">
      Choose a format:<br/>
      <html:radio property="format" value="csv"/>Comma separated values<br/>
      <html:radio property="format" value="tab"/>Tab separated values<br/>
    </c:when>
  </c:choose>

  <br/>

  <html:hidden property="pathsString" value="${pathsString}"/>
  <html:hidden property="table" value="${table}"/>
  <html:hidden property="type" value="${type}"/>

  <div id="pathsDiv">
    <c:forEach var="path" items="${paths}" varStatus="status">
      <div class="exportPath">
        ${path}
      </div>
    </c:forEach>
  </div>

  <br clear="both"/>
  <html:submit property="submit"><fmt:message key="export.submit"/></html:submit>
</html:form>

<script type="text/javascript">
  <!--
     Sortable.create('pathsDiv', {
        tag:'div', dropOnEmpty:true,  constraint:'horizontal', overlap:'horizontal', onUpdate:function() {
        //       reorderOnServer();
     }
   });
 -->
</script>
<!-- /exportOptions.jsp -->
