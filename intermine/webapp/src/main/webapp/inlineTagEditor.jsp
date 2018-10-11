<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- inlineTagEditor.jsp -->

<html:xhtml/>

<tiles:importAttribute name="vertical" ignore="true"/>
<tiles:importAttribute name="show" ignore="true"/>
<tiles:importAttribute name="onChangeCode" ignore="true"/>

<c:if test="${vertical}">
  <div>
</c:if>
<%-- editorId is id specific for each InlineTagEditor - used for creating unique javascript ids --%>
<%-- The hidden input stores the name of the element we are referring to --%>
<form><input type="hidden" id="taggable-${editorId}" value="${taggableIdentifer}"></form>
<span id="tags-${editorId}" style="${!show?'display:none':''}">
  <span id="currentTags-${editorId}" class="current-tags">
    <tiles:insert page="/currentTags.jsp"></tiles:insert>
  </span>
  <span id="addLink-${editorId}">
    <a class="addTagLink" onclick="javascript:startEditingTag('${editorId}')" >Add tags</a>
  </span>
  <span>
    <a class="addTagLink" onclick="javascript:switchTagInput('${editorId}')" style="display:none;" id="switchLink-${editorId}">New tag</a>
  </span>
  <span id="tagsEdit-${editorId}" style="display:none; white-space:nowrap">
    <c:if test="${vertical}">
      <br/>
    </c:if>
    <input type="text" style="border: 1px solid #888; padding: 2px; font-size: 10px" size="18" id="tagValue-${editorId}" name="tag"/>
    <tiles:insert name="tagSelect.tile">
            <tiles:put name="type" value="${type}" />
            <tiles:put name="selectId" value="tagSelect-${editorId}" />
            <tiles:put name="tags" beanName="availableTags"/>
            <tiles:put name="title" value="-- select a tag --"/>
    </tiles:insert>
    <input type="button" style="font-size: 10px" value="Add" onclick="addTag('${editorId}', '${type}');${onChangeCode};"/>
    <input type="button" style="font-size: 10px" value="Done" onclick="stopEditingTag('${editorId}')"/>
  </span>
</span>

<c:if test="${vertical}">
  </div>
</c:if>
<!-- /inlineTagEditor.jsp -->
