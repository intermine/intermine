<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

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

<!-- wsTemplateRow.jsp -->

<c:if test="${!empty makeCheckBoxes}">
  <td>
    <html:multibox property="selected" styleId="${wsCheckBoxId}"
                   onclick="setDeleteDisabledness(this.form, '${type}')">
      <c:out value="${wsName}"/>
    </html:multibox>
  </td>
</c:if>

<c:if test="${showNames}">
  <c:choose>
    <c:when test="${!webSearchable.valid}">
      <td align="left" nowrap>
        <html:link action="/templateProblems?name=${wsName}&amp;scope=user" styleClass="brokenTmplLink">
          <strike>${webSearchable.name}</strike>
        </html:link>
      </td>
    </c:when>
    <c:otherwise>
      <td>
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
      </td>
    </c:otherwise>
  </c:choose>
</c:if>

<c:if test="${showTitles}">
  <td>
    ${webSearchable.title}
  </td>
</c:if>

<c:if test="${showDescriptions}">
  <td>
    <c:choose>
      <c:when test="${fn:length(webSearchable.description) > 60}">
        ${fn:substring(webSearchable.description, 0, 60)}...
      </c:when>
      <c:otherwise>
        ${webSearchable.description}
      </c:otherwise>
    </c:choose>
    &nbsp;
  </td>
  <td>
    <c:choose>
      <c:when test="${fn:length(webSearchable.comment) > 60}">
        ${fn:substring(webSearchable.comment, 0, 60)}...
      </c:when>
      <c:otherwise>
        ${webSearchable.comment}
      </c:otherwise>
    </c:choose>
    &nbsp;
  </td>
</c:if>

<td align="center" nowrap>
  <html:link action="/template?name=${webSearchable.name}&amp;scope=${scope}"
             titleKey="history.action.execute.hover">
    <fmt:message key="history.action.execute"/>
  </html:link> |
  <html:link action="/editTemplate?name=${webSearchable.name}&amp;scope=${scope}"
             titleKey="history.action.edit.hover">
    <fmt:message key="history.action.edit"/>
  </html:link> |
  <html:link action="/exportTemplates?scope=${scope}&amp;name=${webSearchable.name}"
             titleKey="history.action.export.hover">
    <fmt:message key="history.action.export"/>
  </html:link>
  <c:if test="${IS_SUPERUSER && webSearchable.valid}">
    <tiles:insert name="precomputeTemplate.tile">
      <tiles:put name="templateName" value="${webSearchable.name}"/>
    </tiles:insert>
    <tiles:insert name="summariseTemplate.tile">
      <tiles:put name="templateName" value="${webSearchable.name}"/>
    </tiles:insert>
  </c:if>
</td>

<!-- /wsTemplateRow.jsp -->
