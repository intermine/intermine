<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!-- wsBagTable.jsp -->
<tiles:importAttribute name="scope"/>
<tiles:importAttribute name="showDescriptions" ignore="true"/>
<tiles:importAttribute name="limit" ignore="true"/>
<tiles:importAttribute name="height" ignore="true"/>
<tiles:importAttribute name="tags" ignore="true"/>
<tiles:importAttribute name="makeCheckBoxes" ignore="true"/>
<tiles:importAttribute name="showSearchBox" ignore="true"/>

<html:xhtml/>
<div class="webSearchable">
<c:choose>
   <c:when test="${scope == 'global'}">
   <h2><fmt:message key="bagspage.public.title"/></h2>
   <p>
     <fmt:message key="bagspage.public.help"/>
   </p>
   </c:when>
   <c:otherwise>
   <h2><fmt:message key="bagspage.mybags.title"/></h2>
   <p>
     <fmt:message key="bagspage.mybags.help"/>
   </p>
   </c:otherwise>
</c:choose>

<tiles:insert name="webSearchableList.tile">
  <tiles:put name="type" value="bag"/>
  <tiles:put name="scope" value="${scope}"/>
  <tiles:put name="tags" value="${tags}"/>
  <tiles:put name="showNames" value="${showNames}"/>
  <tiles:put name="showTitles" value="${showTitles}"/>
  <tiles:put name="showDescriptions" value="${showDescriptions}"/>
  <tiles:put name="makeCheckBoxes" value="${makeCheckBoxes}"/>
  <tiles:put name="makeTable" value="true"/>
  <tiles:put name="header" value="wsBagHeader.tile"/>
  <tiles:put name="row" value="wsBagRow.tile"/>
  <tiles:put name="limit" value="${limit}"/>
  <tiles:put name="height" value="${height}"/>
  <tiles:put name="showSearchBox" value="${showSearchBox}"/>
</tiles:insert>
<c:if test="${(fn:length(PROFILE.savedBags) > 0) && (scope == 'user')}">
  <p width="100%" align="right">
    <html:submit property="delete">
      <fmt:message key="history.delete"/>
    </html:submit>
  </p>
</c:if>
</div>
<!-- /wsBagTable.jsp -->
