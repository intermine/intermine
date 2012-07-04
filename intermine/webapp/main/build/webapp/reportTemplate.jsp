<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

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

<c:set var="templateName" value="${templateQuery.name}"/>
<c:set var="uid" value="${fn:replace(placement, ' ', '_')}_${templateName}"/>
<c:set var="placementAndField" value="${placement}_${templateName}"/>

<c:choose>
	<c:when test="${reportObject != null || interMineIdBag != null}">
		<c:set var="tmlType" value="report"/>
	</c:when>
	<c:otherwise>
		<c:set var="tmlType" value="aspect"/>
	</c:otherwise>
</c:choose>

<div class="template" id="${fn:replace(uid, ":", "_")}">
  <im:templateLine scope="${scope}" templateQuery="${templateQuery}" interMineObject="${interMineObject}" bagName="${interMineIdBag.name}" trail="${trail}" templateType="${tmlType}" />
  <p class="description" style="display:none;">${templateQuery.description}</p>

  <%-- JS target for the table --%>
  <div class="collection-table"></div>

  <script type="text/javascript">
    <c:choose>
      <c:when test="${reportObject != null}">
        <c:set var="interMineObject" value="${reportObject.object}"/>
        queueInlineTemplateQuery('${placement}', '${templateName}', '${reportObject.object.id}', '${trail}');
      </c:when>
      <c:when test="${interMineIdBag != null}">
        queueInlineTemplateQuery('${placement}', '${templateName}', '${interMineIdBag.name}', '${trail}');
      </c:when>
    </c:choose>
  </script>
</div>

<!-- /reportTemplate.jsp -->
