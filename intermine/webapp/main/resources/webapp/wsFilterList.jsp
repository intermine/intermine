<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<!-- wsFilterList.jsp -->

<link rel="stylesheet" type="text/css" href="css/webSearchableList.css"/>

<tiles:importAttribute name="type"/>
<tiles:importAttribute name="scope"/>
<tiles:importAttribute name="showNames" ignore="true"/>
<tiles:importAttribute name="showTitles" ignore="true"/>
<tiles:importAttribute name="showDescriptions" ignore="true"/>
<tiles:importAttribute name="showSearchBox" ignore="true"/>

<tiles:importAttribute name="makeCheckBoxes" ignore="true"/>
<tiles:importAttribute name="makeTable" ignore="true"/>
<tiles:importAttribute name="makeLine" ignore="true"/>
<tiles:importAttribute name="wsHeader" ignore="true"/>
<tiles:importAttribute name="wsRow" ignore="true"/>
<tiles:importAttribute name="height" ignore="true"/>
<tiles:importAttribute name="limit" ignore="true"/>

<html:xhtml/>

<p style="white-space:nowrap;">Filter:&nbsp;
  <input type="text" id="${scope}_${type}_filter_text" name="newName_${name}" size="20" 
         onkeyup="return filterWebSearchables(this, '${scope}', '${type}');" />
  &nbsp;&nbsp;&nbsp;&nbsp;Sort/Filter:&nbsp;
  <img src="images/filter_favourites_ico.gif" width="16" height="16" alt="Show Only Favourites"/>
  &nbsp;
  <img src="images/asc.gif" width="17" height="16" alt="Sort alphabetically"/>
  &nbsp;
  <img src="images/sort_date_ico.gif" width="20" height="16" alt="Sort by Date"/>
</p>

<script type="text/javascript">
  <%-- turn off autocomplete because of a Gecko bug:
       http://geekswithblogs.net/shahedul/archive/2006/08/14/87910.aspx --%>
  <!--
      $('${scope}_${type}_filter_text').setAttribute('autocomplete','off');
    -->
</script>

<tiles:insert name="webSearchableList.tile">
  <tiles:put name="type" value="${type}"/>
  <tiles:put name="scope" value="${scope}"/>
  <tiles:put name="tags" value="${tags}"/>
  <tiles:put name="showNames" value="${showNames}"/>
  <tiles:put name="showTitles" value="${showTitles}"/>
  <tiles:put name="showDescriptions" value="${showDescriptions}"/>
  <tiles:put name="makeCheckBoxes" value="${makeCheckBoxes}"/>
  <tiles:put name="makeTable" value="${makeTable}"/>
  <tiles:put name="makeLine" value="${makeLine}"/>
  <tiles:put name="wsHeader" value="${wsHeader}"/>
  <tiles:put name="wsRow" value="${wsRow}"/>
  <tiles:put name="limit" value="${limit}"/>
  <tiles:put name="height" value="${height}"/>
  <tiles:put name="showSearchBox" value="${showSearchBox}"/>
</tiles:insert>
<!-- /wsFilterList.jsp -->
