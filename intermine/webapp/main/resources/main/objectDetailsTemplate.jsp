<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- objectDetailsTemplate.jsp -->

<html:xhtml/>

<tiles:importAttribute name="displayObject" ignore="true"/>
<tiles:importAttribute name="templateQuery"/>
<tiles:importAttribute name="aspect"/>
<tiles:importAttribute name="type"/>

<c:set var="templateName" value="${templateQuery.name}"/>
<c:set var="uid" value="${fn:replace(aspect, ' ', '_')}_${templateName}"/>

<c:set var="aspectAndField" value="${aspect}_${templateName}"/>
<c:if test="${!empty displayObject}">
  <c:set var="verbose" value="${!empty displayObject.verbosity[aspectAndField]}"/>
  <c:set var="interMineObject" value="${displayObject.object}"/>
</c:if>

<div class="templateLine">
  <c:choose>
    <c:when test="${empty displayObject}">

    </c:when>
    <%--<c:when test="${!empty templateCounts[templateName] &&
                  templateCounts[templateName] == 0}">
      <img border="0" src="images/blank.gif" alt=" " width="11" height="11"/>
      <c:set var="cssClass" value="nullStrike"/>
    </c:when>--%>
    <%--<c:when test="${empty table}">
      <img border="0" src="images/blank.gif" alt=" " width="11" height="11"/>
    </c:when>--%>
    <c:when test="${verbose}">
      <html:link action="/modifyDetails?method=unverbosify&amp;field=${templateName}&amp;aspect=${aspect}&amp;id=${object.id}&amp;trail=${param.trail}"
        onclick="return toggleTemplateList('${fn:replace(aspect, ' ', '_')}', '${templateName}')">
        <img border="0" src="images/minus.gif" alt="-" id="img_${uid}" height="11" width="11"/>
      </html:link>
    </c:when>
    <c:otherwise>
      <html:link action="/modifyDetails?method=verbosify&amp;field=${templateName}&amp;aspect=${aspect}&amp;id=${object.id}&amp;trail=${param.trail}"
        onclick="return toggleTemplateList('${fn:replace(aspect, ' ', '_')}', '${templateName}')">
        <img border="0" src="images/plus.gif" alt="+" id="img_${uid}" height="11" width="11"/>
      </html:link>
    </c:otherwise>
  </c:choose>
  <span class="${cssClass}" id="label_${uid}">
    <im:templateLine type="${type}" templateQuery="${templateQuery}"
                     className="${className}" interMineObject="${interMineObject}"/>
    <span id="count_${uid}" class="templateResCount"></span><br/>
  </span>

  <div id="table_${uid}" style="${verbose?'':'display: none'}">
    <div id="table_${uid}_int">
      <c:if test="${verbose}">
        <tiles:insert name="objectDetailsTemplateTable.tile">
          <tiles:put name="displayObject" beanName="displayObject"/>
          <tiles:put name="templateQuery" beanName="templateQuery"/>
          <tiles:put name="aspect" value="${aspect}"/>
        </tiles:insert>
      </c:if>
    </div>
  </div>
  
  <c:if test="${IS_SUPERUSER}">
    <tiles:insert name="inlineTagEditor.tile">
      <tiles:put name="taggable" beanName="templateQuery"/>
      <tiles:put name="vertical" value="true"/>
      <tiles:put name="show" value="true"/>
    </tiles:insert>
  </c:if>
  
  <c:if test="${!verbose}">
    <script>
      <!--
        $('img_${uid}').src='images/spinner.gif';
        new Ajax.Updater('table_${uid}_int', '<html:rewrite action="/modifyDetails"/>', {
          parameters:'method=ajaxTemplateCount&template=${templateName}&object=${displayObject.object.id}&type=global&aspect=${aspect}',
          onComplete: function() {
            var count = $('count_${uid}').innerHTML;
            if (count == '0')
              $('img_${uid}').src='images/blank.gif';
            else
              $('img_${uid}').src='images/plus.gif';
          },
          evalScripts: true
        });
      -->
    </script>
  </c:if>
  
</div>
<!-- /objectDetailsTemplate.jsp -->
