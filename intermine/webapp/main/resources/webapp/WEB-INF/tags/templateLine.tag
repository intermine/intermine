<%@ tag body-content="empty"  %>

<%@ attribute name="scope" required="true" %>
<%@ attribute name="templateQuery" required="true" type="org.intermine.api.template.TemplateQuery" %>
<%@ attribute name="interMineObject" required="false" type="java.lang.Object" %>
<%@ attribute name="name" required="false" type="java.lang.String" %>
<%@ attribute name="descr" required="false" type="java.lang.String" %>
<%@ attribute name="bagName" required="false" type="java.lang.String" %>
<%@ attribute name="trail" required="false" type="java.lang.String" %>

<%@ include file="/shared/taglibs.jsp" %>

<c:if test="${!templateQuery.valid}">
  <html:link action="/templateProblems?name=${templateQuery.name}&amp;scope=${scope}" styleClass="brokenTmplLink">
    <strike><span class="templateTitle"><c:out value="${templateQuery.title}"/></span></strike>
    <img border="0" class="arrow" src="images/template_t.gif" title="This is an invalid template."/>
  </html:link>
</c:if>
<c:if test="${templateQuery.valid}">

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
  <html:link action="${actionLink}" title="${linkTitle}">
    <span class="templateTitle">${!empty name ? name : templateQuery.title}</span>
  </html:link>
  <fmt:message var="linkTitle" key="templateList.run">
    <fmt:param value="${templateQuery.name}"/>
  </fmt:message>
 
  <%-- favourites star --%>
  <tiles:insert name="setFavourite.tile">
    <tiles:put name="name" value="${templateQuery.name}"/>
    <tiles:put name="type" value="template"/>
  </tiles:insert>
  
  <%-- (t) img.  trail isn't used here because queries always reset the trail --%>
  <html:link action="${runTemplateActionLink}" title="${linkTitle}">
    <img border="0" class="arrow" src="images/template_t.gif" title="Click here to go to the template form"/>
  </html:link>
  <%-- description --%>
  <c:if test="${! empty descr}">
  <br>
  ${descr}
  </c:if>
</c:if>
<c:if test="${scope == 'user'}">
  <%-- pull required messages --%>
  <fmt:message var="confirmMessage" key="templateList.deleteMessage">
    <fmt:param value="${templateQuery.name}"/>
  </fmt:message>
  <fmt:message var="linkTitle" key="templateList.delete">
    <fmt:param value="${templateQuery.name}"/>
  </fmt:message>
  <%-- map of parameters to pass to the confirm action --%>
  <jsp:useBean id="deleteParams" scope="page" class="java.util.TreeMap">
    <c:set target="${deleteParams}" property="message" value="${confirmMessage}" />
    <c:set target="${deleteParams}" property="confirmAction" value="/userTemplateAction?method=delete&amp;name=${templateQuery.name}&amp;type=${templateType}" />
    <c:set target="${deleteParams}" property="cancelAction" value="/begin" />
  </jsp:useBean>
  <html:link action="/confirm" name="deleteParams" title="${linkTitle}">
    <img border="0" class="arrow" src="images/cross.gif" title="Click here to confirm"/>
  </html:link>
  <c:remove var="deleteParams"/>
  <c:if test="${templateQuery.valid}">
    <fmt:message var="linkTitle" key="templateList.edit">
      <fmt:param value="${templateQuery.name}"/>
    </fmt:message>
    <html:link action="/editTemplate?name=${templateQuery.name}" title="${linkTitle}">
      <img border="0" class="arrow" src="images/edit.gif" title="Click here to edit"/>
    </html:link>
  </c:if>
</c:if>
