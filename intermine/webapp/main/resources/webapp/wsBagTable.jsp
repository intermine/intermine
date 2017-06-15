<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>


<!-- wsBagTable.jsp -->
<tiles:importAttribute name="scope"/>
<tiles:importAttribute name="wsListId"/>
<tiles:importAttribute name="showDescriptions" ignore="true"/>
<tiles:importAttribute name="showTags" ignore="true"/>
<tiles:importAttribute name="limit" ignore="true"/>
<tiles:importAttribute name="height" ignore="true"/>
<tiles:importAttribute name="tags" ignore="true"/>
<tiles:importAttribute name="makeCheckBoxes" ignore="true"/>
<tiles:importAttribute name="showSearchBox" ignore="true"/>

<html:xhtml/>

<fmt:message key="bags.title" var="titleKey" />

<im:boxarea titleImage="lists-64.png" title="${titleKey}" stylename="gradientbox" minWidth="800px" htmlId="liststop">
<div class="" id="ws_${wsListId}_bag"><fmt:message key="lists.intro"/>

<tiles:insert name="wsFilterList.tile">
  <tiles:put name="type" value="bag"/>
  <tiles:put name="scope" value="${scope}"/>
  <tiles:put name="wsListId" value="${wsListId}"/>
  <tiles:put name="tags" value="${tags}"/>
  <tiles:put name="showNames" value="${showNames}"/>
  <tiles:put name="showTitles" value="${showTitles}"/>
  <tiles:put name="showDescriptions" value="${showDescriptions}"/>
  <tiles:put name="showTags" value="${showTags}"/>
  <tiles:put name="makeCheckBoxes" value="${makeCheckBoxes}"/>
  <tiles:put name="makeTable" value="false"/>
  <tiles:put name="makeLine" value="true"/>
  <%--<tiles:put name="wsHeader" value="wsBagHeader.tile"/>--%>
  <tiles:put name="wsRow" value="wsBagLine.tile"/>
  <tiles:put name="limit" value="${limit}"/>
  <tiles:put name="height" value="${height}"/>
  <tiles:put name="showSearchBox" value="${showSearchBox}"/>
  <tiles:put name="loginMessageKey" value="lists.notlogged"/>
  <tiles:put name="showCount" value="true"/>
</tiles:insert>
<html:link anchor="liststop" action="bag"><img src="images/go_to_top.png" title="Click here to jump to the top of the page"/>top</html:link>
</div>
</im:boxarea>
<!-- /wsBagTable.jsp -->
