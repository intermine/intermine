<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1" prefix="str" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<tiles:importAttribute name="wsName"/>
<tiles:importAttribute name="scope"/>
<tiles:importAttribute name="wsListId"/>
<tiles:useAttribute id="webSearchable" name="webSearchable" 
                    classname="org.intermine.web.logic.search.WebSearchable"/>
<tiles:importAttribute name="showDescriptions" ignore="true"/>
<tiles:importAttribute name="statusIndex"/>
<tiles:importAttribute name="makeCheckBoxes" ignore="true"/>

<c:set var="type" value="bag"/>

<!-- wsBagLine.jsp -->
<div id="${wsListId}_${type}_item_line_${webSearchable.name}" <c:choose><c:when test="${! empty userWebSearchables[wsName]}">class="wsLine_my" onmouseout="this.className='wsLine_my'" onmouseover="this.className='wsLine_my_act'"</c:when>
<c:otherwise> class="wsLine" onmouseout="this.className='wsLine'" onmouseover="this.className='wsLine_act'"</c:otherwise></c:choose>>
<div style="float: right" id="${wsListId}_${type}_item_score_${webSearchable.name}">
  &nbsp;
</div>

<c:if test="${!empty makeCheckBoxes}">
    <html:multibox property="selectedBags" styleId="${wsListId}_${type}_chck_${webSearchable.name}">
      <c:out value="${webSearchable.name}" escapeXml="false"/>
    </html:multibox>
</c:if>

  <c:choose>
    <c:when test="${scope == 'user'}">
      <tiles:insert name="renamableElement.jsp">
        <tiles:put name="name" value="${webSearchable.name}"/>
        <tiles:put name="type" value="bag"/>
        <tiles:put name="index" value="${statusIndex}"/>
      </tiles:insert>
    </c:when>
    <c:otherwise>
        <c:set var="nameForURL"/>
        <str:encodeUrl var="nameForURL">${webSearchable.name}</str:encodeUrl>
        <html:link action="/bagDetails?scope=${scope}&amp;bagName=${nameForURL}">
          <c:out value="${webSearchable.name}"/>
          <img src="images/bag_ico.png" width="13" height="13" alt="View Bag">
        </html:link>
    </c:otherwise>
  </c:choose>

<%--<html:link action="/exportTemplates?scope=${scope}&amp;name=${webSearchable.name}"
           titleKey="history.action.export.hover">
  <img src="images/export.png" width="16" height="13" alt="Export">
</html:link>--%>
<tiles:insert name="setFavourite.tile" >
  <tiles:put name="name" value="${webSearchable.name}"/>
  <tiles:put name="type" value="bag"/>
</tiles:insert>


<c:out value="${webSearchable.size}"/>
<b><c:choose>
<c:when test="${webSearchable.size != 1}">
  <c:out value="${webSearchable.type}s" />
</c:when>
<c:otherwise>
  <c:out value="${webSearchable.type}" />
</c:otherwise>
</c:choose></b>
<!-- <em><c:if test="${!empty webSearchable.dateCreated}">
  <im:dateDisplay type="short" date="${webSearchable.dateCreated}"/>
</c:if></em> -->

<c:if test="${showDescriptions}">
  <div id="${wsListId}_${type}_item_description_${webSearchable.name}">
  <c:choose>
   <c:when test="${fn:length(webSearchable.description) > 100}">
     <div id="bag_desc_${webSearchable.name}_s" class="description">
     ${fn:substring(webSearchable.description, 0, 100)}...&nbsp;&nbsp;<a href="javascript:toggleDivs('bag_desc_${webSearchable.name}_s','bag_desc_${webSearchable.name}_l')">more</a>
     </div>
     <div id="bag_desc_${webSearchable.name}_l" style="display:none" class="description">
     ${webSearchable.description}&nbsp;&nbsp;<a href="javascript:toggleDivs('bag_desc_${webSearchable.name}_l','bag_desc_${webSearchable.name}_s')">less</a>
     </div>
   </c:when>
   <c:otherwise>
     <p class="description">${webSearchable.description}</p>
   </c:otherwise>
 </c:choose>
  </div>
   <div id="${wsListId}_${type}_item_description_${webSearchable.name}_highlight" style="display:none" class="description"></div>
</c:if>

<c:if test="${IS_SUPERUSER}">
    <c:set var="taggable" value="${webSearchable}"/>
    <tiles:insert name="inlineTagEditor.tile">
      <tiles:put name="taggable" beanName="taggable"/>
      <tiles:put name="vertical" value="true"/>
      <tiles:put name="show" value="true"/>
    </tiles:insert>
</c:if>
</div>
<!-- /wsBagLine.jsp -->
