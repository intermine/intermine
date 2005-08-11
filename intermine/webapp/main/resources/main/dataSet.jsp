
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- dataSet -->

<html:xhtml/>

<%-- Import into request scope so that popup can see it --%>
<tiles:importAttribute name="dataSet" scope="request"/>

<im:box topLeftTile="/dataSetIcon.jsp" topRightTile="/dataSetPopup.jsp">
<fmt:setBundle var="mb" basename="model"/>

<c:if test="${!empty dataSet.dataSetSources}">
  <div class="dataSetSources">
    <fmt:message key="dataset.${dataSet.name}.sources.from" bundle="${mb}"/>
    <ul>
      <c:forEach items="${dataSet.dataSetSources}" var="source">
        <li><a href="${source.url}" target="_blank" class="extlink">${source.name}</a></li>
      </c:forEach>
    </ul>
  </div>
</c:if>

<div class="body dataSetIntro">
  ${dataSet.introText}
</div>

<c:if test="${!empty dataSet.tileName}">
  <div class="dataSetTile">
    <tiles:insert name="${dataSet.tileName}"/>
  </div>
</c:if>

<div class="heading">
  <fmt:message key="dataset.starting.points.heading"/>
</div>

<div class="body dataSetStartingPoints">
  <c:forEach items="${dataSet.startingPoints}" var="classname" varStatus="status">
    <a href="<html:rewrite page="/queryClassSelect.do"/>?action=<fmt:message key="button.selectClass"/>&amp;className=${classname}" title="<c:out value="${classDescriptions[classname]}"/>">
       ${classname}</a><c:if test="${!status.last}">,</c:if>
  </c:forEach>
</div>

<c:if test="${!empty CATEGORY_TEMPLATES[dataSet.name]}">
  <div class="heading">
    <fmt:message key="dataset.templates.heading"/>
  </div>
  <div class="body dataSetTemplates">
    <im:templateList type="global" category="${dataSet.name}"/>
  </div>
</c:if>

</im:box>

<!-- /dataSet -->

