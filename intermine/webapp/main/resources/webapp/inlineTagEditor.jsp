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

<script type="text/javascript" src="js/inlineTagEditor.js"></script>

<c:if test="${vertical}">
  <div style="margin-top: 5px">
</c:if>

<span id="tags-${tagged}" style="${!show?'display:none':''}">
  <span id="currentTags-${tagged}">
    <tiles:insert page="/currentTags.jsp">
    </tiles:insert>
  </span>
  <span id="addLink-${tagged}">
    <a class="addTagLink" onclick="javascript:startEditingTag('${tagged}')" >Add tags</a>
  </span>
  <span>
  	<a  class="addTagLink" onclick="javascript:switchTagInput('${tagged}')" style="display:none;" id="switchLink-${tagged}">New tag</a>
  </span>
  <span id="tagsEdit-${tagged}" style="display:none; white-space:nowrap">
    <c:if test="${vertical}">
      <br/>
    </c:if>
    <input type="text" style="border: 1px solid #888; padding: 2px; font-size: 10px" size="18" id="tagValue-${tagged}" name="tag"
      onKeyPress="if(event.keyCode == 13) {addTag('${tagged}', '${type}');$('tagValue-${tagged}').focus();${onChangeCode};}"/>
    <tiles:insert name="tagSelect.tile">
            <tiles:put name="type" value="${type}" />
            <tiles:put name="selectId" value="tagSelect-${tagged}" />
            <tiles:put name="tags" beanName="availableTags"/>
    </tiles:insert>
    <input type="button" style="font-size: 10px" value="Add" onclick="addTag('${tagged}', '${type}');${onChangeCode};$('tagValue-${tagged}').focus();"/>
    <input type="button" style="font-size: 10px" value="Done" onclick="stopEditingTag('${tagged}')"/>
  </span>
</span>

<c:if test="${vertical}">
  </div>
</c:if>
<!-- /inlineTagEditor.jsp -->
