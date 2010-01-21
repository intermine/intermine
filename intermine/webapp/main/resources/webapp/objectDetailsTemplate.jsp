<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- objectDetailsTemplate.jsp -->

<html:xhtml/>

<tiles:importAttribute name="displayObject" ignore="true"/>
<tiles:importAttribute name="interMineIdBag" ignore="true"/>
<tiles:importAttribute name="templateQuery"/>
<tiles:importAttribute name="placement"/>
<tiles:importAttribute name="scope"/>
<tiles:importAttribute name="trail"/>

<c:set var="templateName" value="${templateQuery.name}"/>
<c:set var="uid" value="${fn:replace(placement, ' ', '_')}_${templateName}"/>

<c:set var="placementAndField" value="${placement}_${templateName}"/>
<c:if test="${!empty displayObject}">
  <c:set var="verbose" value="${!empty displayObject.verbosity[placementAndField]}"/>
  <c:set var="interMineObject" value="${displayObject.object}"/>
</c:if>

<c:if test="${!empty param.trail}">
  <c:set var="trail" value="${param.trail}"/>
</c:if>

<div class="templateLine">
  <c:choose>
    <c:when test="${empty displayObject && empty interMineIdBag}">
      <%-- no icon at all --%>
    </c:when>

    <c:when test="${verbose}">
      <div class="templateIcon">
        <a href="#" onClick="return toggleTemplateList('${fn:replace(placement, ' ', '_')}', '${templateName}')">
          <img border="0" src="images/minus.gif" title="Click here to hide these data" id="img_${uid}" height="11" width="11"/>
        </a>
      </div>
    </c:when>
    <c:otherwise>
      <div class="templateIcon">
        <a href="#" onClick="return toggleTemplateList('${fn:replace(placement, ' ', '_')}', '${templateName}')">
          <img border="0" src="images/plus.gif" title="Click here to show these data" id="img_${uid}" height="11" width="11"/>
        </a>
      </div>
    </c:otherwise>
  </c:choose>

  <%--title line e.g. "+ Gene --> Proteins.  [STAR] (t)  1 results" --%>
  <div class="templateDetails">
    <span class="${cssClass}" id="label_${uid}">
      <im:templateLine scope="${scope}" templateQuery="${templateQuery}"
                       interMineObject="${interMineObject}" bagName="${interMineIdBag.name}" trail="${trail}" />
      <span id="count_${uid}" class="templateResCount"></span><br/>
    </span>
  </div>

 <%--the "N results" bit is located at the bottom of objectDetailsTemplateTable.jsp for some reason--%>

  <%--results table--%>
  <div id="table_${uid}" style="${verbose?'':'display: none'}">
    <div class="templateDescription" style="width 90%">
      ${templateQuery.description}
    </div>
    <div id="table_${uid}_int">
      <c:if test="${verbose}">
        <tiles:insert name="objectDetailsTemplateTable.jsp">
          <tiles:put name="displayObject" beanName="displayObject"/>
          <tiles:put name="interMineIdBag" beanName="interMineIdBag"/>
          <tiles:put name="templateQuery" beanName="templateQuery"/>
          <tiles:put name="placement" value="${placement}"/>
        </tiles:insert>
      </c:if>
    </div>
  </div>
  
  <c:choose>
    <c:when test="${!verbose && displayObject != null}">
      <script type="text/javascript">
      <!--//<![CDATA[      
          $('img_${uid}').src='images/spinner.gif';
          queueInlineTemplateQuery('${placement}', '${templateName}', '${displayObject.object.id}', '${trail}');
        //]]>-->
          
      </script>
    </c:when>
    <c:when test="${!verbose && interMineIdBag != null}">
      <script type="text/javascript">
      <!--//<![CDATA[
          $('img_${uid}').src='images/spinner.gif';
          queueInlineTemplateQuery('${placement}', '${templateName}', '${interMineIdBag.name}', '${trail}');
        //]]>-->
          
      </script>
    </c:when>
  </c:choose>
  
</div>
<!-- /objectDetailsTemplate.jsp -->
