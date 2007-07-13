<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- inlineTagEditor.jsp -->

<html:xhtml/>

<tiles:importAttribute name="vertical" ignore="true"/>
<tiles:importAttribute name="show" ignore="true"/>
<link rel="stylesheet" type="text/css" href="css/inlineTagEditor.css"/>

<c:if test="${vertical}">
  <div style="margin-top: 5px">
</c:if>

<img class="tag" src="images/tag.gif" onclick="new Effect.Appear('tags-${uid}', {duration: 0.15})"/>

<span id="tags-${uid}" style="${!show?'display:none':''}">
  <span id="currentTags-${uid}">
    <tiles:insert page="/currentTags.jsp"/>
  </span>
  <span id="addLink-${uid}">
    <a class="addTagLink" href="#" onclick="startEditingTag('${uid}');return false">Add tags</a>
  </span>
  <span id="tagsEdit-${uid}" style="display:none; white-space:nowrap">
    <c:if test="${vertical}">
      <br/>
    </c:if>
    <!--<input type="hidden" name="tag-uid" value="${uid}"/>
    <input type="hidden" name="tag-type" value="${type}"/>-->
    <input type="text" style="border: 1px solid #888; padding: 2px; font-size: 10px" size="18" id="tagValue-${uid}" name="tag"
      onKeyPress="if(event.keyCode == 13) {addTag('${uid}', '${type}');$('tagValue-${uid}').focus();return false;}"/>
    <input type="button" style="font-size: 10px" value="Add" onclick="addTag('${uid}', '${type}');$('tagValue-${uid}').focus();"/>
    <input type="button" style="font-size: 10px" value="Done" onclick="stopEditingTag('${uid}')"/>
  </span>
</span>

<c:if test="${vertical}">
  </div>
</c:if>

<!-- /inlineTagEditor.jsp -->
