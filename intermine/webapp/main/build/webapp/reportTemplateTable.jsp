<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- reportTemplateTable.jsp -->

<% if (pageContext.getAttribute("org.apache.struts.taglib.tiles.CompContext", PageContext.REQUEST_SCOPE) != null) { %>
  <tiles:importAttribute/>
<% } %>

<html:xhtml/>

<c:choose>
<c:when test="${(reportObject != null || interMineIdBag !=null) && resultsTable != null}">

<div style="overflow-x:auto; display:none;">
	<%-- results table --%>
  	<tiles:insert name="resultsTable.tile">
    	<tiles:put name="pagedResults" beanName="resultsTable" />
     	<tiles:put name="inlineTable" value="true" />
     	<tiles:put name="currentPage" value="report" />
     	<tiles:put name="tableIdentifier" value="${fn:replace(placement, ':', '_')}_${templateQuery.name}" />
  	</tiles:insert>
</div>

<%-- is? --%>
<c:set var="extra" value=""/>
<c:choose>
  <c:when test="${!empty interMineIdBag}">
    <c:set var="extra" value="&amp;bagName=${interMineIdBag.name}"/>
  </c:when>
  <c:otherwise>
    <c:set var="extra" value="${extra}&amp;idForLookup=${reportObject.object.id}" />
  </c:otherwise>
</c:choose>

<%-- more or less show in table --%>
<div class="toggle"></div>

<div class="show-in-table" style="display:none;">
<c:choose>
  <c:when test="${resultsTable.exactSize == 0}">
    <%-- postdict the fact that we have nothing to show --%>
    <script type="text/javascript">
        if ('${reportObject.type}'.length > 0) {
          var text = 'No results for this ${reportObject.type}';
        } else {
          var text = 'No results for this type';
        }

	    jQuery('#${fn:replace(placement, ':', '_')}_${templateQuery.name} h3').find('div.right').text(text).parent().parent().addClass('gray');
    </script>
  </c:when>
  <c:otherwise>
    <html:link action="/modifyDetails?method=runTemplate&amp;name=${templateQuery.name}&amp;scope=global${extra}&amp;trail=${param.trail}">
      Show all in a table »
    </html:link>
  </c:otherwise>
</c:choose>
</div>

</c:when>
<c:otherwise>
  <script type="text/javascript">
    throw new Error('${templateQuery.name} has failed to load, resultsTable || imIDBag || reportObject are null');
  </script>
</c:otherwise>
</c:choose>

<!-- /reportTemplateTable.jsp -->
