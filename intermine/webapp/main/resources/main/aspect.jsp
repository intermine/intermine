
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- aspect -->

<html:xhtml/>

<%-- Import into request scope so that popup can see it --%>
<tiles:importAttribute name="aspect" scope="request"/>
<tiles:importAttribute name="startingPoints"/>

<im:box title="${aspect.name}" topRightTile="/aspectPopup.jsp">

<c:if test="${!empty aspect.aspectSources}">
  <div class="aspectSources">
    <fmt:message key="aspect.sources.from"/>
    <ul>
      <c:forEach items="${aspect.aspectSources}" var="source">
        <li><a href="${source.url}" target="_blank" class="extlink">${source.name}</a></li>
      </c:forEach>
    </ul>
  </div>
</c:if>

<div class="body aspectIntro">
  <p>
    <img src="${ASPECTS[aspect.name].iconImage}" class="aspectPageIcon"/>
    ${aspect.introText}
  </p>
</div>
<div style="clear:both;"></div>

<c:if test="${!empty aspect.tileName}">
  <div class="aspectTile">
    <tiles:insert name="${aspect.tileName}"/>
  </div>
</c:if>

<div class="heading">
  <fmt:message key="aspect.starting.points.heading"/>
</div>

<div class="body aspectStartingPoints">
  <c:forEach items="${startingPoints}" var="classname" varStatus="status">
    <im:unqualify className="${classname}" var="name"/>
    <a href="<html:rewrite page="/queryClassSelect.do"/>?action=<fmt:message key="button.selectClass"/>&amp;className=${classname}" title="<c:out value="${classDescriptions[classname]}"/>">${name}</a><c:if test="${!status.last}">,</c:if>
  </c:forEach>
</div>

<%--<c:if test="${!empty CATEGORY_TEMPLATES[aspect.name]}">--%>
  <div class="heading">
    <fmt:message key="aspect.templates.heading"/>
  </div>
  <div class="body aspectTemplates">
    <tiles:insert name="templateList.tile">
      <tiles:put name="type" value="global"/>
      <tiles:put name="placement" value="aspect:${aspect.name}"/>
      <tiles:put name="noTemplatesMsgKey" value="templateList.noTemplates"/>
    </tiles:insert>
  </div>
<%--</c:if>--%>

</im:box>

<!-- /aspect -->

