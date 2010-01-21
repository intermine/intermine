<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil" %>

<tiles:importAttribute name="displayObject" ignore="true" />
<tiles:importAttribute name="interMineIdBag" ignore="true" />
<tiles:importAttribute name="aspectId" ignore="true" />
<tiles:importAttribute name="placement" />
<tiles:importAttribute name="trail" />
<tiles:importAttribute name="opened" ignore="true" />

<!-- objectDetailsAspect.jsp -->

<c:forEach items="${displayObject.clds}" var="cld">
  <c:if
    test="${fn:length(WEBCONFIG.types[cld.name].aspectDisplayers[placement]) > 0}">
    <c:set var="foundDisplayer" value="true" />
  </c:if>
</c:forEach>

<c:choose>
<c:when test="${!empty placementRefsAndCollections[placement] || foundDisplayer == true || 
    !empty templates}">
    <c:set var="aspect" value="${fn:replace(placement, 'im:aspect:', '')}" scope="request" />
    <c:set var="templateCount" value="${fn:length(templates)}" />
    <c:choose>
      <c:when test="${templateCount > 0}">
        <c:set var="templateHeaderMsg" value="(Expand this section to view all ${templateCount} templates)" />
      </c:when>
      <c:otherwise>
        <c:set var="templateHeaderMsg" value="(Expand this section for more information)" />
      </c:otherwise>
    </c:choose>

    <imutil:disclosure id="${aspectId}" type="consistent" opened="${opened}">
      <imutil:disclosureHead>
        <imutil:disclosureTitle>${aspect}</imutil:disclosureTitle>
        <imutil:disclosureDetails styleClass="templateResultsToggle">${templateHeaderMsg}</imutil:disclosureDetails>
      </imutil:disclosureHead>
      <imutil:disclosureBody>
      	  <div style="margin-left:-1px;">
	      	  <c:if test="${!empty displayObject}">
    	          <tiles:insert page="/objectDetailsRefsCols.jsp">
	                   <tiles:put name="object" beanName="displayObject" />
	                   <tiles:put name="placement" value="${placement}" />
	              </tiles:insert>
	          </c:if> 
	       </div>
          <%-- the controller of this tile should have already called the controller
            for the template list so just insert the jsp page --%> 
        <c:if test="${!empty templates && !empty placementRefsAndCollections[placement]}">
          <hr class="seperator" />
        </c:if> 
        <tiles:insert name="/templateList.jsp">
          <tiles:put name="scope" value="global" />
          <tiles:put name="placement" value="${placement}" />
          <tiles:put name="displayObject" beanName="displayObject" />
          <tiles:put name="interMineIdBag" beanName="interMineIdBag" />
          <tiles:put name="noTemplatesMsgKey" value="" />
          <tiles:put name="trail" value="${trail}" />
        </tiles:insert> 
        <c:if test="${!empty templates && foundDisplayer}">
          <hr class="seperator" />
        </c:if> 
        <c:if test="${! empty displayObject}">
          <tiles:insert page="/objectDetailsDisplayers.jsp">
            <tiles:put name="placement" value="${placement}" />
            <tiles:put name="displayObject" beanName="displayObject" />
          </tiles:insert>
        </c:if> 
        <im:vspacer height="5" />
      </imutil:disclosureBody>
    </imutil:disclosure>
  </c:when>
  <c:otherwise>
    <!-- nothing to display for placement ${placement} -->
  </c:otherwise>
</c:choose>
<!-- /objectDetailsAspect.jsp -->
