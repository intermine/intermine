<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1" prefix="str" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>

<tiles:importAttribute name="wsName"/>
<tiles:importAttribute name="scope"/>
<tiles:importAttribute name="wsListId"/>
<tiles:useAttribute id="webSearchable" name="webSearchable"
                    classname="org.intermine.api.search.WebSearchable"/>
<tiles:importAttribute name="showDescriptions" ignore="true"/>
<tiles:importAttribute name="showTags" ignore="true"/>
<tiles:importAttribute name="statusIndex"/>
<tiles:importAttribute name="makeCheckBoxes" ignore="true"/>

<c:set var="type" value="bag"/>

<!-- wsBagLine.jsp -->
<div id="${wsListId}_${type}_item_line_${webSearchable.name}" <c:choose><c:when test="${! empty userWebSearchables[wsName]}">class="wsLine_my" onmouseout="this.className='wsLine_my'" onmouseover="this.className='wsLine_my_act'"</c:when>
<c:otherwise> class="wsLine" onmouseout="swapStyles('${wsListId}_${type}_item_line_${webSearchable.name}','wsLine','wsLine_act','${wsListId}_${type}_chck_${webSearchable.name}')" onmouseover="swapStyles('${wsListId}_${type}_item_line_${webSearchable.name}','wsLine','wsLine_act','${wsListId}_${type}_chck_${webSearchable.name}')"</c:otherwise></c:choose>>
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
        &nbsp;<html:link styleClass="listTitle" action="/bagDetails?scope=${scope}&amp;bagName=${nameForURL}">
         <%--  <img src="images/bag_ico.png" width="13" height="13" alt="View Bag"> --%>
          <c:out value="${webSearchable.name}"/>
        </html:link>
    </c:otherwise>
  </c:choose>

<c:if test="${! empty sharedBagWebSearchables[wsName]}"><i> shared by ${sharedBagWebSearchables[wsName]}</i></c:if>
<tiles:insert name="setFavourite.tile" >
  <tiles:put name="name" value="${webSearchable.name}"/>
  <tiles:put name="type" value="bag"/>
</tiles:insert>

<c:set var="typeDisplayName" value="${imf:formatPathStr(webSearchable.type, INTERMINE_API, WEBCONFIG)}"/>

<c:out value="${webSearchable.size}"/>&nbsp;<b><c:choose>
<c:when test="${webSearchable.size != 1}">
  <c:out value="${typeDisplayName}s" />
</c:when>
<c:otherwise>
  <c:out value="${typeDisplayName}" />
</c:otherwise>
</c:choose></b>
<!-- <em><c:if test="${!empty webSearchable.dateCreated}">
  <im:dateDisplay type="short" date="${webSearchable.dateCreated}"/>
</c:if></em> -->

<c:if test="${showDescriptions}">
  <div id="${wsListId}_${type}_item_description_${webSearchable.name}">
     <p class="description">${webSearchable.description}</p>
  </div>
   <div id="${wsListId}_${type}_item_description_${webSearchable.name}_highlight" style="display:none" class="description"></div>
</c:if>

<c:if test="${showTags}">
  <div id="${wsListId}_${type}_item_tags_${webSearchable.name}">
     <p class="description">
        <c:set var="taggable" value="${webSearchable}"/>
        <tiles:insert name="listTags.tile" >
            <tiles:put name="taggable" beanName="taggable"/>
        </tiles:insert>
     </p>
  </div>
</c:if>

</div>
<!-- /wsBagLine.jsp -->
