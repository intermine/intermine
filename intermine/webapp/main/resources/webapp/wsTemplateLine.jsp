<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<tiles:importAttribute name="wsListId"/>
<tiles:importAttribute name="wsName"/>
<tiles:useAttribute id="webSearchable" name="webSearchable"
                    classname="org.intermine.api.search.WebSearchable"/>
<tiles:importAttribute name="scope"/>
<tiles:importAttribute name="showNames" ignore="true"/>
<tiles:importAttribute name="showTitles" ignore="true"/>
<tiles:importAttribute name="showDescriptions" ignore="true"/>
<tiles:importAttribute name="statusIndex"/>
<tiles:importAttribute name="makeCheckBoxes" ignore="true"/>

<c:set var="type" value="template"/>

<!-- wsTemplateLine.jsp -->
<div id="${wsListId}_${type}_item_line_${webSearchable.name}" <c:choose><c:when test="${! empty userWebSearchables[wsName]}">class="wsLine_my" onmouseout="this.className='wsLine_my'" onmouseover="this.className='wsLine_my_act'"</c:when>
<c:otherwise> class="wsLine" onmouseout="swapStyles('${wsListId}_${type}_item_line_${webSearchable.name}','wsLine','wsLine_act','${wsListId}_${type}_chck_${webSearchable.name}')" onmouseover="swapStyles('${wsListId}_${type}_item_line_${webSearchable.name}','wsLine','wsLine_act','${wsListId}_${type}_chck_${webSearchable.name}')"</c:otherwise></c:choose> >
<div style="float: right" id="${wsListId}_${type}_item_score_${webSearchable.name}">
  &nbsp;
</div>
<c:if test="${!empty makeCheckBoxes}">
    <html:multibox property="selected" styleId="${wsListId}_${type}_chck_${webSearchable.name}">
      <c:out value="${wsName}"/>
    </html:multibox>
</c:if>

<c:if test="${showNames}">
  <c:choose>
    <c:when test="${!webSearchable.valid}">
        <html:link action="/templateProblems?name=${wsName}&amp;scope=user" styleClass="brokenTmplLink">
          <strike>${webSearchable.name}</strike>
        </html:link>
    </c:when>
    <c:otherwise>

        <fmt:message var="linkTitle" key="templateList.run">
    <fmt:param value="${webSearchable.name}"/>
        </fmt:message>

        ${webSearchable.name}
        <tiles:insert name="setFavourite.tile">
          <tiles:put name="name" value="${webSearchable.name}"/>
          <tiles:put name="type" value="template"/>
        </tiles:insert>
        <c:if test="${IS_SUPERUSER}">
          <c:set var="taggable" value="${webSearchable}"/>
          <tiles:insert name="inlineTagEditor.tile">
            <tiles:put name="taggable" beanName="taggable"/>
            <tiles:put name="vertical" value="true"/>
            <tiles:put name="show" value="true"/>
          </tiles:insert>
        </c:if>
    </c:otherwise>
  </c:choose>
</c:if>
<c:if test="${showTitles}">
    <html:link styleClass="templateTitle" action="/template?name=${webSearchable.name}&amp;scope=${scope}"
                 titleKey="history.action.execute.hover">${webSearchable.title}</html:link>
</c:if>

<tiles:insert name="setFavourite.tile">
  <tiles:put name="name" value="${webSearchable.name}"/>
  <tiles:put name="type" value="template"/>
</tiles:insert>

<c:if test="${showDescriptions}">
   <div id="${wsListId}_${type}_item_description_${webSearchable.name}">
    <p class="description">${webSearchable.description}</p>
   </div>
   <div id="${wsListId}_${type}_item_description_${webSearchable.name}_highlight" style="display:none" class="description"></div>
</c:if>

  <c:if test="${IS_SUPERUSER && webSearchable.valid}">
<br><u>Superuser actions</u>:
    <html:link action="/editTemplate?name=${webSearchable.name}&amp;scope=${scope}"
             titleKey="history.action.edit.hover">
      <fmt:message key="history.action.edit"/>
    </html:link>
    <tiles:insert name="precomputeTemplate.tile">
      <tiles:put name="templateName" value="${webSearchable.name}"/>
      <tiles:put name="precomputedTemplateMap" beanName="precomputedTemplateMap" />
    </tiles:insert>
    <tiles:insert name="summariseTemplate.tile">
      <tiles:put name="templateName" value="${webSearchable.name}"/>
      <tiles:put name="summarisedTemplateMap" beanName="summarisedTemplateMap" />
    </tiles:insert>
  </c:if>
</div>
<!-- /wsTemplateLine.jsp -->
