
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

<im:box topLeftTile="/aspectIcon.jsp" topRightTile="/aspectPopup.jsp">

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
  ${aspect.introText}
</div>

<c:if test="${!empty aspect.tileName}">
  <div class="aspectTile">
    <tiles:insert name="${aspect.tileName}"/>
  </div>
</c:if>

<div class="heading">
  <fmt:message key="aspect.starting.points.heading"/>
</div>

<div class="body aspectStartingPoints">
  <c:forEach items="${aspect.startingPoints}" var="classname" varStatus="status">
    <a href="<html:rewrite page="/queryClassSelect.do"/>?action=<fmt:message key="button.selectClass"/>&amp;className=${classname}" title="<c:out value="${classDescriptions[classname]}"/>">
       ${classname}</a><c:if test="${!status.last}">,</c:if>
  </c:forEach>
</div>

<c:if test="${!empty CATEGORY_TEMPLATES[aspect.name]}">
  <div class="heading">
    <fmt:message key="aspect.templates.heading"/>
  </div>
  <div class="body aspectTemplates">
    <im:templateList type="global" category="${aspect.name}"/>
  </div>
</c:if>

</im:box>

<!-- /aspect -->

