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

<c:set var="templateName" value="${templateQuery.name}"/>
<c:set var="uid" value="${fn:replace(placement, ' ', '_')}_${templateName}"/>

<c:set var="placementAndField" value="${placement}_${templateName}"/>
<c:if test="${!empty reportObject}">
  <c:set var="verbose" value="false"/>
  <c:set var="interMineObject" value="${reportObject.object}"/>
</c:if>

<c:if test="${!empty param.trail}">
  <c:set var="trail" value="${param.trail}"/>
</c:if>

<div class="template table">
  <div class="templateDetails">
    <span class="${cssClass}" id="label_${fn:replace(uid, ":", "_")}">
      <im:templateLine scope="${scope}" templateQuery="${templateQuery}"
                       interMineObject="${interMineObject}" bagName="${interMineIdBag.name}" trail="${trail}" />
      <span id="count_${uid}" class="templateResCount"></span>
    </span>
    <p class="description theme-1-color theme-5-background">${templateQuery.description}</p>
  </div>

 <%--the "N results" bit is located at the bottom of reportTemplateTable.jsp for some reason--%>

  <%--results table--%>
  <div id="table_${fn:replace(uid, ":", "_")}">
    <div id="table_${fn:replace(uid, ":", "_")}_int">
      <c:if test="${verbose}">
        <tiles:insert name="reportTemplateTable.jsp">
          <tiles:put name="reportObject" beanName="reportObject"/>
          <tiles:put name="interMineIdBag" beanName="interMineIdBag"/>
          <tiles:put name="templateQuery" beanName="templateQuery"/>
          <tiles:put name="placement" value="${placement}"/>
        </tiles:insert>
      </c:if>
      <p class='loading'>&nbsp;</p>
    </div>
  </div>

  <c:choose>
    <c:when test="${!verbose && reportObject != null}">
      <script type="text/javascript">
          queueInlineTemplateQuery('${placement}', '${templateName}', '${reportObject.object.id}', '${trail}');
      </script>
    </c:when>
    <c:when test="${!verbose && interMineIdBag != null}">
      <script type="text/javascript">
          queueInlineTemplateQuery('${placement}', '${templateName}', '${interMineIdBag.name}', '${trail}');
      </script>
    </c:when>
  </c:choose>

</div>
<!-- /reportTemplate.jsp -->
