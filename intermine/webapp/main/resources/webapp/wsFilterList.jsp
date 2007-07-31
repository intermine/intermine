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

<c:set var="ws_input_id" value="${scope}_${type}_filter_text"/>
<c:set var="ws_input_aspect" value="${scope}_${type}_filter_aspect"/>

<p style="white-space:nowrap;">Filter:&nbsp;
  <input type="text" id="${ws_input_id}" name="newName_${name}" size="20" 
         onkeyup="return filterWebSearchablesHandler(event, this, '${scope}', '${type}', null);"
         onmouseup="return filterWebSearchablesHandler(event, this, '${scope}', '${type}', null);"
         disabled="true"/>
  &nbsp; <img id='${scope}_${type}_spinner' style='visibility: hidden' 
             src='images/wait_spinner.gif'/>
  &nbsp;&nbsp;&nbsp;Sort/Filter:&nbsp;
<c:if test="${! empty PROFILE.username}">
  <a href="javascript:filterFavourites('${scope}','${type}');"><img id="filter_favourites_${scope}_${type}" src="images/filter_favourites_ico.gif" width="16" height="16" alt="Show Only Favourites" title="Show Only Favourites"/></a>
  &nbsp;
</c:if>
  <img src="images/asc.gif" width="17" height="16" alt="Sort by name"/>
  &nbsp;
  <img src="images/sort_date_ico.gif" width="20" height="16" alt="Sort by Date"/>
  <input type="hidden" name="filterAction_${scope}_${type}" id="filterAction_${scope}_${type}"/>
 
  <%-- aspects --%>
    <select onchange="javascript:filterAspect('${scope}','${type}')" id="${ws_input_aspect}" class="aspectSelect">
    <c:if test="${aspect == null}">
      <option value="" selected>-- Choose aspect --</option>
    </c:if>
    <c:forEach items="${ASPECTS}" var="entry">
      <c:set var="set" value="${entry.value}"/>
      <option value="${set.name}"
        <c:if test="${aspect.name == set.name}">
          selected
        </c:if>
      >${set.name}</option>
    </c:forEach>
  </select>
  
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

  <script type="text/javascript">
<%-- enable filter only after the list is populated --%>
<!--//<![CDATA[
    $('${ws_input_id}').disabled = false;
//]]>-->
  </script>

<!-- /wsFilterList.jsp -->
