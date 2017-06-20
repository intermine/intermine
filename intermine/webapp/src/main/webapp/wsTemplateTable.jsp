<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<!-- wsTemplateTable.jsp -->
<tiles:importAttribute name="filter" ignore="true"/>
<tiles:importAttribute name="wsListId"/>
<tiles:importAttribute name="scope"/>
<tiles:importAttribute name="showNames" ignore="true"/>
<tiles:importAttribute name="showTitles" ignore="true"/>
<tiles:importAttribute name="showDescriptions" ignore="true"/>
<tiles:importAttribute name="limit" ignore="true"/>
<tiles:importAttribute name="height" ignore="true"/>
<tiles:importAttribute name="tags" ignore="true"/>
<tiles:importAttribute name="makeCheckBoxes" ignore="true"/>
<tiles:importAttribute name="showSearchBox" ignore="true"/>
<tiles:importAttribute name="templatesPublicPage" ignore="true"/>


<html:xhtml/>

<im:boxarea titleImage="templates-64.png" titleKey="menu.templates" stylename="gradientbox" minWidth="800px" htmlId="templatetop">
<div style="">
<fmt:message key="templates.intro"/>
<%--
<br/>
<b>
<fmt:message key="templates.mostpopular">
<fmt:param value="${mostPopularTemplate}"/>
</fmt:message>
</b>
--%>
</div>
<div class="" id="ws_${wsListId}_template">

<tiles:insert name="wsFilterList.tile">
  <tiles:put name="type" value="template"/>
  <tiles:put name="wsListId" value="${wsListId}"/>
  <tiles:put name="scope" value="${scope}"/>
  <tiles:put name="tags" value="${tags}"/>
  <tiles:put name="showNames" value="${showNames}"/>
  <tiles:put name="showTitles" value="${showTitles}"/>
  <tiles:put name="showDescriptions" value="${showDescriptions}"/>
  <tiles:put name="makeCheckBoxes" value="${makeCheckBoxes}"/>
  <tiles:put name="initialFilterText" value="${filter}"/>
  <tiles:put name="makeLine" value="true"/>
  <tiles:put name="makeTable" value="false"/>
  <tiles:put name="templatesPublicPage" value="${templatesPublicPage}"/>
  <!-- <tiles:put name="wsHeader" value="wsTemplateHeader.tile"/> -->
  <tiles:put name="wsRow" value="wsTemplateLine.tile"/>
  <tiles:put name="limit" value="${limit}"/>
  <tiles:put name="height" value="${height}"/>
  <tiles:put name="showSearchBox" value="${showSearchBox}"/>
  <tiles:put name="loginMessageKey" value="template.notlogged"/>
  <tiles:put name="showCount" value="false"/>
</tiles:insert>
<html:link anchor="templatetop" action="templates" styleClass="anchor"><img src="images/go_to_top.png" title="Click here to jump to the top of this page">top</html:link>
</div>
</im:boxarea>

<!-- /wsTemplateTable.jsp -->
