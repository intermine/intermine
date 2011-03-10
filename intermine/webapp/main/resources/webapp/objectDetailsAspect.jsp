<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil"%>

<tiles:importAttribute name="displayObject" ignore="true" />
<tiles:importAttribute name="interMineIdBag" ignore="true" />
<tiles:importAttribute name="aspectId" ignore="true" />
<tiles:importAttribute name="placement" />
<tiles:importAttribute name="mapOfInlineLists" ignore="false" />
<tiles:importAttribute name="trail" />
<tiles:importAttribute name="opened" ignore="true" />

<!-- objectDetailsAspect.jsp -->

<c:forEach items="${displayObject.clds}" var="cld">
  <c:if
    test="${fn:length(WEBCONFIG.types[cld.name].aspectDisplayers[placement]) > 0}">
    <c:set var="foundDisplayer" value="true" />
  </c:if>
</c:forEach>

<c:if
  test="${!empty placementRefsAndCollections[placement] || foundDisplayer == true ||
    !empty templates}">
  <c:set var="aspect" value="${fn:replace(placement, 'im:aspect:', '')}"
    scope="request" />
  <c:set var="templateCount" value="${fn:length(templates)}" />

  <a name="<c:out value="${fn:toLowerCase(aspect)}"/>"><h2>${aspect}</h2></a>
  <c:if test="${!empty displayObject}">
    <tiles:insert page="/objectDetailsNormalInlineLists.jsp">
      <tiles:put name="mapOfInlineLists" beanName="mapOfInlineLists" />
      <tiles:put name="placement" value="${placement}" />
    </tiles:insert>
  </c:if>
  <div>
    <c:if test="${!empty displayObject}">
      <tiles:insert page="/objectDetailsRefsCols.jsp">
        <tiles:put name="object" beanName="displayObject" />
        <tiles:put name="placement" value="${placement}" />
      </tiles:insert>
    </c:if>
  </div>
  <tiles:insert name="/templateList.jsp">
    <tiles:put name="scope" value="global" />
    <tiles:put name="placement" value="${placement}" />
    <tiles:put name="displayObject" beanName="displayObject" />
    <tiles:put name="interMineIdBag" beanName="interMineIdBag" />
    <tiles:put name="noTemplatesMsgKey" value="" />
    <tiles:put name="trail" value="${trail}" />
  </tiles:insert>
  <c:if test="${! empty displayObject}">
    <tiles:insert page="/objectDetailsDisplayers.jsp">
      <tiles:put name="placement" value="${placement}" />
      <tiles:put name="displayObject" beanName="displayObject" />
    </tiles:insert>
  </c:if>

</c:if>
<!-- /objectDetailsAspect.jsp -->
