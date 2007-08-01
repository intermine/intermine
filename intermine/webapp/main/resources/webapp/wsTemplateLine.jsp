<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<tiles:importAttribute name="wsListId"/>
<tiles:importAttribute name="wsName"/>
<tiles:useAttribute id="webSearchable" name="webSearchable"
                    classname="org.intermine.web.logic.search.WebSearchable"/>
<tiles:importAttribute name="scope"/>
<tiles:importAttribute name="showNames" ignore="true"/>
<tiles:importAttribute name="showTitles" ignore="true"/>
<tiles:importAttribute name="showDescriptions" ignore="true"/>
<tiles:importAttribute name="statusIndex"/>
<tiles:importAttribute name="wsCheckBoxId" ignore="true"/>
<tiles:importAttribute name="makeCheckBoxes" ignore="true"/>

<c:set var="type" value="template"/>

<!-- wsTemplateLine.jsp -->
<div class="wsTempateLine" id="${wsListId}_${type}_item_line_${webSearchable.name}">
<div style="float: right" id="${wsListId}_${type}_item_score_${webSearchable.name}">
  &nbsp;
</div>
<c:if test="${!empty makeCheckBoxes}">
    <html:multibox property="selected" styleId="${wsListId}_template_chck_${webSearchable.name}"
                   onclick="setDeleteDisabledness(this.form, '${type}')">
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
    <html:link action="/template?name=${webSearchable.name}&amp;scope=${scope}"
                 titleKey="history.action.execute.hover">${webSearchable.title}</html:link>
</c:if>

<html:link action="/template?name=${webSearchable.name}&amp;scope=${scope}"
           titleKey="history.action.execute.hover">
  <img src="images/template_t.gif" width="13" height="13" alt="Run Template">
</html:link>
<html:link action="/exportTemplates?scope=${scope}&amp;name=${webSearchable.name}"
           titleKey="history.action.export.hover">
  <img src="images/export.png" width="16" height="13" alt="Export">
</html:link>
<tiles:insert name="setFavourite.tile">
  <tiles:put name="name" value="${webSearchable.name}"/>
  <tiles:put name="type" value="template"/>
</tiles:insert>

<c:if test="${showDescriptions}">
   <div id="${wsListId}_${type}_item_description_${webSearchable.name}">
     <c:choose>
       <c:when test="${fn:length(webSearchable.description) > 60}">
         <div id="temp_desc_${webSearchable.name}_s" class="description">
           ${fn:substring(webSearchable.description, 0, 60)}...&nbsp;&nbsp;<a href="javascript:toggleDivs('temp_desc_${webSearchable.name}_s','temp_desc_${webSearchable.name}_l')">more</a>
         </div>
         <div id="temp_desc_${webSearchable.name}_l" style="display:none" class="description">
           ${webSearchable.description}&nbsp;&nbsp;<a href="javascript:toggleDivs('temp_desc_${webSearchable.name}_l','temp_desc_${webSearchable.name}_s')">less</a>
         </div>
       </c:when>
       <c:otherwise>
         <p class="description">${webSearchable.description}</p>
       </c:otherwise>
     </c:choose>
   </div>
    <%--<c:choose>
      <c:when test="${fn:length(webSearchable.comment) > 60}">
        ${fn:substring(webSearchable.comment, 0, 60)}...
      </c:when>
      <c:otherwise>
        ${webSearchable.comment}
      </c:otherwise>
    </c:choose>
    &nbsp;--%>

</c:if>

  <c:if test="${IS_SUPERUSER && webSearchable.valid}">
<br><u>Superuser actions</u>:
    <html:link action="/editTemplate?name=${webSearchable.name}&amp;scope=${scope}"
             titleKey="history.action.edit.hover">
      <fmt:message key="history.action.edit"/>
    </html:link>
    <tiles:insert name="precomputeTemplate.tile">
      <tiles:put name="templateName" value="${webSearchable.name}"/>
    </tiles:insert>
    <tiles:insert name="summariseTemplate.tile">
      <tiles:put name="templateName" value="${webSearchable.name}"/>
    </tiles:insert>
  </c:if>
</div>
<!-- /wsTemplateLine.jsp -->
