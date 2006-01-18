<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<tiles:importAttribute name="displayObject"/>
<tiles:importAttribute name="aspect"/>

<!-- objectDetailsAspect.jsp -->

<c:forEach items="${displayObject.clds}" var="cld">
  <c:if test="${fn:length(WEBCONFIG.types[cld.name].aspectDisplayers[aspect]) > 0}">
    <c:set var="foundDisplayer" value="true"/>
  </c:if>
</c:forEach>

<c:choose>
  <c:when test="${!empty aspectRefsAndCollections[aspect] ||
              foundDisplayer == true ||
              !empty templates}">

  <im:heading id="${aspect}">
    ${aspect}<%--<im:helplink key="objectDetails.help.otherInfo"/>--%>
  </im:heading>
    <im:body id="${aspect}">
      <tiles:insert page="/objectDetailsAspectRefsCols.jsp">
        <tiles:put name="object" beanName="displayObject"/>
        <tiles:put name="aspect" value="${aspect}"/>
      </tiles:insert>
      <%-- the controller of this tile should have already called the controller
        for the template list so just insert the jsp page --%>
      <tiles:insert name="/templateList.jsp">
        <tiles:put name="type" value="global"/>
        <tiles:put name="aspect" value="${aspect}"/>
        <tiles:put name="displayObject" beanName="displayObject"/>
        <tiles:put name="noTemplatesMsgKey" value=""/>
      </tiles:insert>
      <tiles:insert page="/objectDetailsDisplayers.jsp">
        <tiles:put name="aspect" value="${aspect}"/>
        <tiles:put name="displayObject" beanName="displayObject"/>
      </tiles:insert>
      <im:vspacer height="5"/>
    </im:body>
  </c:when>
  <c:otherwise>
    <!-- nothing to display for aspect ${aspect} -->
  </c:otherwise>
</c:choose>


<!-- /objectDetailsAspect.jsp -->

