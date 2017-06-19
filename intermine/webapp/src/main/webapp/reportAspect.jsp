<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil"%>

<tiles:importAttribute name="reportObject" ignore="true" />
<tiles:importAttribute name="interMineIdBag" ignore="true" />
<tiles:importAttribute name="aspectId" ignore="true" />
<tiles:importAttribute name="placement" ignore="false"/>
<tiles:importAttribute name="mapOfInlineLists" ignore="true" />
<tiles:importAttribute name="trail" />
<tiles:importAttribute name="opened" ignore="true" />

<!-- reportAspect.jsp -->

<c:set var="aspect" value="${fn:replace(placement, 'im:aspect:', '')}" scope="request" />

<c:if test="${!empty reportObject}">
  <c:if test="${fn:length(reportObject.reportDisplayers[aspect]) > 0}">
    <c:set var="foundDisplayer" value="true" />
  </c:if>
</c:if>

<c:if
  test="${!empty placementRefsAndCollections[placement] || foundDisplayer == true ||
    !empty templates}">
  <c:set var="templateCount" value="${fn:length(templates)}" />

  <c:if test="${aspect != 'im:summary' && aspect != 'summary'}">
  	<a name="<c:out value="${fn:toLowerCase(fn:replace(aspect, ' ', '_'))}"/>"><h2>${aspect}</h2></a>
  </c:if>
  <c:if test="${!empty reportObject}">
    <tiles:insert page="/reportDisplayers.jsp">
      <tiles:put name="placement" value="${aspect}" />
      <tiles:put name="reportObject" beanName="reportObject" />
    </tiles:insert>

    <tiles:insert page="/reportNormalInlineLists.jsp">
      <tiles:put name="mapOfInlineLists" beanName="mapOfInlineLists" />
      <tiles:put name="placement" value="${placement}" />
    </tiles:insert>
  </c:if>
  <div>
    <c:if test="${!empty reportObject}">
      <tiles:insert page="/reportRefsCols.jsp">
        <tiles:put name="object" beanName="reportObject" />
        <tiles:put name="placement" value="${placement}" />
      </tiles:insert>
    </c:if>
  </div>
  <tiles:insert name="/templateList.jsp">
    <tiles:put name="scope" value="global" />
    <tiles:put name="placement" value="im:aspect:${placement}" />
    <tiles:put name="reportObject" beanName="reportObject" />
    <tiles:put name="interMineIdBag" beanName="interMineIdBag" />
    <tiles:put name="noTemplatesMsgKey" value="" />
    <tiles:put name="trail" value="${trail}" />
  </tiles:insert>
</c:if>
<!-- /reportAspect.jsp -->