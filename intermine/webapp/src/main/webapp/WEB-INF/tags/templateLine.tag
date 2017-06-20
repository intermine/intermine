<%@ tag body-content="empty"  %>

<%@ attribute name="scope" required="true" %>
<%@ attribute name="templateQuery" required="true" type="org.intermine.template.TemplateQuery" %>
<%@ attribute name="interMineObject" required="false" type="java.lang.Object" %>
<%@ attribute name="name" required="false" type="java.lang.String" %>
<%@ attribute name="descr" required="false" type="java.lang.String" %>
<%@ attribute name="bagName" required="false" type="java.lang.String" %>
<%@ attribute name="trail" required="false" type="java.lang.String" %>
<%@ attribute name="templateType" required="false" type="java.lang.String" %>

<%@ include file="/shared/taglibs.jsp" %>

<c:choose>
<c:when test="${!templateQuery.valid}">
  <html:link action="/templateProblems?name=${templateQuery.name}&amp;scope=${scope}" styleClass="brokenTmplLink">
    <strike><span class="templateTitle"><c:out value="${templateQuery.title}"/></span></strike>
    <img border="0" class="arrow" src="images/icons/templates-16.png" title="This is an invalid template."/>
  </html:link>
</c:when>
<c:otherwise>

<c:choose>
  <c:when test="${! empty bagName}">
    <%-- bag page --%>
    <c:set var="extra" value="&amp;bagName=${bagName}&amp;useBagNode=${fieldExprMap[templateQuery]}"/>
  </c:when>
  <c:otherwise>
    <%-- aspect or report page --%>
     <c:set var="extra" value="&amp;idForLookup=${interMineObject.id}" />
  </c:otherwise>
</c:choose>



  <c:set var="extra" value="${extra}&trail=${trail}" />

  <c:set var="runTemplateActionLink" value="/template?name=${templateQuery.name}&amp;scope=${scope}${extra}"/>

  <c:choose>
    <c:when test="${empty bagName && empty interMineObject}">
      <%-- for aspect pages --%>
      <c:set var="actionLink" value="${runTemplateActionLink}"/>
    </c:when>
    <c:otherwise>
      <%-- for object & bag details pages --%>
      <c:set var="actionLink" value="/modifyDetails?method=runTemplate&amp;name=${templateQuery.name}&amp;scope=${scope}${extra}"/>
    </c:otherwise>
  </c:choose>
  <h3>
  <div class="right"></div>
  <img src="images/icons/templates-16.png" />

  <c:choose>
    <c:when test="${ templateType != 'aspect' }">
      <span class="name">${!empty name ? name : templateQuery.title}</span>
    </c:when>
    <c:otherwise>
      <html:link action="${actionLink}" title="${linkTitle}">${!empty name ? name : templateQuery.title}</html:link>
    </c:otherwise>
  </c:choose>

  <div class="favorites">
    <tiles:insert name="setFavourite.tile">
      <tiles:put name="name" value="${templateQuery.name}"/>
      <tiles:put name="type" value="template"/>
    </tiles:insert>
  </div>
  </h3>
  <%--</html:link>--%>
  <fmt:message var="linkTitle" key="templateList.run">
    <fmt:param value="${templateQuery.name}"/>
  </fmt:message>

  <%-- description --%>
  <c:if test="${! empty descr}">
    ${descr}
  </c:if>
</c:otherwise>
</c:choose>
