<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>

<!-- reportTemplate.jsp -->

<html:xhtml/>

<tiles:importAttribute name="reportObject" ignore="true"/>
<tiles:importAttribute name="interMineIdBag" ignore="true"/>
<tiles:importAttribute name="templateQuery"/>
<tiles:importAttribute name="placement"/>
<tiles:importAttribute name="scope"/>
<tiles:importAttribute name="trail"/>

<c:if test="${!empty param.trail}">
  <c:set var="trail" value="${param.trail}"/>
</c:if>

<c:set var="initValue" value="0"/>
<c:if test="${empty currentUniqueId}">
    <c:set var="currentUniqueId" value="${initValue}" scope="application"/>
</c:if>
<c:set var="tableContainerId" value="_unique_id_${currentUniqueId}" scope="request"/>
<c:set var="currentUniqueId" value="${currentUniqueId + 1}" scope="application"/>


<c:set var="templateName" value="${templateQuery.name}"/>
<c:set var="uid" value="${fn:replace(placement, ' ', '_')}_${templateName}"/>
<c:set var="placementAndField" value="${placement}_${templateName}"/>


<c:choose>
    <c:when test="${reportObject != null}">
        <c:set var="query" value="${imf:populateTemplateWithObject(templateQuery, reportObject.object)}"/>
    </c:when>
    <c:when test="${interMineIdBag != null}">
        <c:set var="query" value="${imf:populateTemplateWithBag(templateQuery, interMineIdBag)}"/>
	</c:when>
	<c:otherwise>
		<c:set var="tmlType" value="aspect"/>
	</c:otherwise>
</c:choose>


<c:set var="elemId" value="${fn:replace(uid, ':', '_')}"/>

<div class="template" id="${elemId}">
  <im:templateLine scope="${scope}" templateQuery="${templateQuery}" interMineObject="${interMineObject}" bagName="${interMineIdBag.name}" trail="${trail}" templateType="${tmlType}" />
  <p class="description" style="display:none;">${templateQuery.description}</p>
  
  <%-- JS target for the table --%>
  <div class="collection-table" id="${tableContainerId}"></div>

  <script type="text/javascript">
    (function($) {
        intermine.css.headerIcon = "fm-header-icon";
        var query = ${query.json};

        $(function() {
            $('#${elemId} h3').click(function(e) {
                var view = new intermine.query.results.CompactView($SERVICE, query, LIST_EVENTS, {pageSize: 10});
                view.$el.appendTo('#${tableContainerId}');
                view.render();
                $(this).unbind('click').click(function(e) {
                    $('#${tableContainerId}').slideToggle('fast');
                });
            });
        });
    }).call(window, jQuery);
  </script>
</div>

<!-- /reportTemplate.jsp -->
