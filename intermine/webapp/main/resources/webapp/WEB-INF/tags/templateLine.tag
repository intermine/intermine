<%@ tag body-content="empty"  %>

<%@ attribute name="scope" required="true" %>
<%@ attribute name="templateQuery" required="true" type="org.intermine.web.logic.template.TemplateQuery" %>
<%@ attribute name="interMineObject" required="false" type="java.lang.Object" %>
<%@ attribute name="desc" required="false" type="java.lang.String" %>
<%@ attribute name="bagName" required="false" type="java.lang.String" %>

<%@ include file="/shared/taglibs.jsp" %>

<c:if test="${!templateQuery.valid}">
  <html:link action="/templateProblems?name=${templateQuery.name}&amp;scope=${scope}" styleClass="brokenTmplLink">
    <strike><span class="templateTitle"><c:out value="${templateQuery.title}"/></span></strike>
    <img border="0" class="arrow" src="images/template_t.gif" alt="->"/>
  </html:link>
</c:if>
<c:if test="${templateQuery.valid}">
  <c:set var="extra" value=""/>
  <c:if test="${!empty fieldExprMap}">
    <c:forEach items="${fieldExprMap[templateQuery]}" var="fieldExpr">
      <c:set var="fieldName" value="${fn:split(fieldExpr, '.')[1]}"/>
      <c:set var="fieldValue" value="${interMineObject[fieldName]}"/>
      <c:set var="extra" value="${extra}&amp;${fieldExpr}_value=${fieldValue}"/>
    </c:forEach>
  </c:if>
  <c:if test="${!empty bagName}">
    <c:set var="extra" value="${extra}&amp;bagName=${bagName}" />
  </c:if>
  <%-- if bagName empty and fieldExprMap too then it's probably a LOOKUP template --%>
  <c:if test="${empty bagName && empty fieldExprMap}">
    <c:set var="extra" value="${extra}&amp;idForLookup=${object.object.id}" />
  </c:if>
  
  <html:link action="/template?name=${templateQuery.name}&amp;scope=${scope}${extra}" 
             title="${linkTitle}">
    <span class="templateTitle">${!empty desc ? desc : templateQuery.title}</span>
  </html:link>
  <fmt:message var="linkTitle" key="templateList.run">
    <fmt:param value="${templateQuery.name}"/>
  </fmt:message>
 
  <%-- favourites star --%>
  <tiles:insert name="starTemplate.tile">
    <tiles:put name="templateName" value="${templateQuery.name}"/>
  </tiles:insert>
  
  <%-- (t) img.  trail isn't used here because queries always reset the trail --%>
  <html:link action="/template?name=${templateQuery.name}&amp;scope=${scope}${extra}" 
             title="${linkTitle}">
    <img border="0" class="arrow" src="images/template_t.gif" alt="-&gt;"/>
  </html:link>
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
    <img border="0" class="arrow" src="images/cross.gif" alt="x"/>
  </html:link>
  <c:remove var="deleteParams"/>
  <c:if test="${templateQuery.valid}">
    <fmt:message var="linkTitle" key="templateList.edit">
      <fmt:param value="${templateQuery.name}"/>
    </fmt:message>
    <html:link action="/editTemplate?name=${templateQuery.name}" title="${linkTitle}">
      <img border="0" class="arrow" src="images/edit.gif" alt="[edit]"/>
    </html:link>
  </c:if>
</c:if>
