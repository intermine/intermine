<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!-- webSearchableList.jsp -->

<tiles:importAttribute name="type"/>
<tiles:importAttribute name="scope"/>
<tiles:importAttribute name="wsListId"/>
<tiles:importAttribute name="showNames" ignore="true"/>
<tiles:importAttribute name="showTitles" ignore="true"/>
<tiles:importAttribute name="showDescriptions" ignore="true"/>
<tiles:importAttribute name="loginMessageKey" ignore="true"/>

<link rel="stylesheet" type="text/css" href="css/webSearchableList.css"/>

<%-- if true this tile must be inside a <form> otherwise this tomcat error
     will appear in the log:
     Cannot find bean under name org.apache.struts.taglib.html.BEAN
  --%>
<tiles:importAttribute name="makeCheckBoxes" ignore="true"/>

<%-- if true, show the WebSearchables in a table - wsRow must be set too --%>
<tiles:importAttribute name="makeTable" ignore="true"/>

<%-- if true, show the WebSearchables in a line - wsRow must be set too --%>
<tiles:importAttribute name="makeLine" ignore="true"/>

<%-- the tile to use for the header of a table - should contain <th> elements --%>
<tiles:importAttribute name="wsHeader" ignore="true"/>

<%-- the tile to use for showing a single row of a table - should contain <td>
     elements as it will be wrapped in a <tr> if makeTable is true--%>
<tiles:importAttribute name="wsRow" ignore="true"/>

<%-- setting height causes the tile to be wrapped in div of the given height
     and with overflow: auto set --%>
<tiles:importAttribute name="height" ignore="true"/>

<%-- if true, keep the spinner visible until showWSList() is called --%>
<tiles:importAttribute name="delayDisplay" ignore="true"/>


<html:xhtml/>

<%-- set default to true --%>
<c:if test="${empty showNames}">
  <c:set var="showNames" value="true" scope="request"/>
</c:if>
<c:if test="${empty showTitles}">
  <c:set var="showTitles" value="true" scope="request"/>
</c:if>
<c:if test="${empty showDescriptions}">
  <c:set var="showDescriptions" value="true" scope="request"/>
</c:if>

<c:set var="heightStyle" value=""/>
<c:set var="spinnerPaddingStyle" value=""/>
<c:set var="containerStyle" value=""/>

<c:if test="${!empty height && fn:length(filteredWebSearchables) > 5}">
  <c:set var="heightStyle" value="height: ${height}px; "/>
  <c:set var="spinnerPaddingStyle" value="padding-top: ${height / 2 - 20}px"/>
</c:if>

<div style="${heightStyle} overflow: auto; background-color: white">

  <noscript>
	<c:set var="spinnerPaddingStyle" value="display:none"/>
  </noscript>

<div style="${spinnerPaddingStyle}" id="${wsListId}_${type}_spinner" class="wsListSpinner"><img src="images/wait30.gif" title="Searching..."/></div>

<c:choose>
  <c:when test="${empty filteredWebSearchables}">
    <div class="altmessage" id="${wsListId}_${type}_no_matches" >
      [no matches found]
    </div>
  </c:when>
  <c:otherwise>
    <div class="altmessage" id="${wsListId}_${type}_no_matches" style="${spinnerPaddingStyle};display:none">
      [no matches found]
    </div>
  </c:otherwise>
</c:choose>

<div id="${wsListId}_${type}_container" class="wsListContainer">

    <div id='${wsListId}_${type}_ws_list' class="wsList">

<c:if test="${!empty loginMessageKey}">
  <c:if test="${empty PROFILE_MANAGER || empty PROFILE.username}">
    <div class="notLogged">
      <em>
        <fmt:message key="${loginMessageKey}">
          <fmt:param>
            <im:login/>
          </fmt:param>
        </fmt:message>
      </em>
    </div>
  </c:if>
</c:if>
      
      <c:choose>
        <c:when test="${!empty makeTable && makeTable}">
          <%-- make a table --%>
          <table>
            <c:if test="${!empty wsHeader}">
              <thead>
                <tr>
                  <tiles:insert name="${wsHeader}">
                    <tiles:put name="scope" value="${scope}"/>
                    <tiles:put name="wsListId" value="${wsListId}"/>
                    <tiles:put name="tags" value="${tags}"/>
                    <tiles:put name="makeCheckBoxes" value="${makeCheckBoxes}"/>
                    <tiles:put name="showNames" value="false"/>
                    <tiles:put name="showTitles" value="false"/>
                    <tiles:put name="showNames" value="${showNames}"/>
                    <tiles:put name="showTitles" value="${showTitles}"/>
                    <tiles:put name="showDescriptions" value="${showDescriptions}"/>
                  </tiles:insert>
                </tr>
              </thead>
            </c:if>
            <tbody>
              <c:forEach items="${filteredWebSearchables}" var="entry" varStatus="status">
                <c:set var="webSearchable" value="${entry.value}" scope="request"/>
                <tr id='${wsListId}_${type}_item_${entry.value.name}' class="${ageClasses[entry.key]}">
                  <tiles:insert name="${wsRow}">
                    <tiles:put name="wsListId" value="${wsListId}"/>
                    <tiles:put name="wsName" value="${entry.key}"/>
                    <tiles:put name="webSearchable" beanName="webSearchable"/>
                    <tiles:put name="statusIndex" value="${status.index}"/>
                    <tiles:put name="wsCheckBoxId"
                               value="selected_${wsListId}_${type}_${status.index}"/>
                    <tiles:put name="makeCheckBoxes" value="${makeCheckBoxes}"/>
                    <tiles:put name="scope" value="${scope}"/>
                    <tiles:put name="tags" value="${tags}"/>
                    <tiles:put name="showNames" value="${showNames}"/>
                    <tiles:put name="showTitles" value="${showTitles}"/>
                    <tiles:put name="showDescriptions" value="${showDescriptions}"/>
                  </tiles:insert>
                </tr>
              </c:forEach>
            </tbody>
          </table>
        </c:when>
        <c:when test="${!empty makeLine && makeLine}">
        <%-- make a line --%>
        <c:forEach items="${filteredWebSearchables}" var="entry" varStatus="status">
          <c:set var="webSearchable" value="${entry.value}" scope="request"/>
            <tiles:insert name="${wsRow}">
              <tiles:put name="wsListId" value="${wsListId}"/>
              <tiles:put name="wsName" value="${entry.key}"/>
              <tiles:put name="webSearchable" beanName="webSearchable"/>
              <tiles:put name="statusIndex" value="${status.index}"/>
              <tiles:put name="makeCheckBoxes" value="${makeCheckBoxes}"/>
              <tiles:put name="scope" value="${scope}"/>
              <tiles:put name="tags" value="${tags}"/>
              <tiles:put name="showNames" value="${showNames}"/>
              <tiles:put name="showTitles" value="${showTitles}"/>
              <tiles:put name="showDescriptions" value="${showDescriptions}"/>
            </tiles:insert>
          </c:forEach>
        </c:when>
        <c:otherwise>
          <%-- make a list --%>
          <ul>
            <c:forEach items="${filteredWebSearchables}" var="entry">
              <li id='${wsListId}_${type}_item_${entry.value.name}'>
                <div class="wsListElement">
                  <html:link action="/gotows?type=${type}&amp;scope=${scope}&amp;name=${entry.key}">${entry.value.title} 
                  </html:link>
                  <tiles:insert name="setFavourite.tile" flush="false">
                    <tiles:put name="name" value="${entry.value.title}"/>
                    <tiles:put name="type" value="${type}"/>
                  </tiles:insert>
                <c:if test="${showDescriptions}">
                  <div class="wsListDescription">
                    ${entry.value.description}
                  </div>
                </c:if>
               </div>
              </li>
            </c:forEach>
          </ul>
        </c:otherwise>
      </c:choose>
    </div>

</div>

<script type="text/javascript">
<!--//<![CDATA[
    function showWSList(wsListId, type) {
        $(wsListId + '_' + type + '_spinner').style.display = 'none';
        $(wsListId + '_' + type + '_container').style.display = 'block';
    }

    setWsNamesMap(${wsNames}, '${wsListId}', '${type}');
//]]>-->
</script>

<c:if test="${empty delayDisplay || !delayDisplay}">
  <script type="text/javascript">
<!--//<![CDATA[
    showWSList('${wsListId}', '${type}');
//]]>-->
  </script>
</c:if>

</div>
<!-- /webSearchableList.jsp -->
