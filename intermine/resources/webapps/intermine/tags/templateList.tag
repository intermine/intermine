<%@ tag body-content="empty"  %>

<%@ attribute name="category" required="true" %>
<%@ attribute name="type" required="true" %>
<%@ attribute name="className" required="false" %>
<%@ attribute name="interMineObject" required="false" type="java.lang.Object" %>

<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<c:if test="${type == 'global' && empty className}">
  <c:set var="templates" value="${CATEGORY_TEMPLATES[category]}"/>
</c:if>
<c:if test="${type == 'global' && !empty className}">
  <c:set var="templates" value="${CLASS_CATEGORY_TEMPLATES[className][category]}"/>
</c:if>
<c:if test="${type == 'user'}">
  <c:set var="templates" value="${PROFILE.categoryTemplates[category]}"/>
</c:if>

<c:forEach items="${templates}" var="templateQuery" varStatus="status">
  <span class="templateDesc"><c:out value="${templateQuery.description}"/></span>&nbsp;
  <fmt:message var="linkTitle" key="templateList.run">
    <fmt:param value="${templateQuery.name}"/>
  </fmt:message>
  <c:set var="extra" value=""/>
  <c:if test="${!empty className}">
    <c:forEach items="${CLASS_TEMPLATE_EXPRS[className][templateQuery.name]}" var="fieldExpr">
      <c:set var="fieldName" value="${fn:split(fieldExpr, '.')[1]}"/>
      <c:set var="fieldValue" value="${interMineObject[fieldName]}"/>
      <c:set var="extra" value="${extra}&${fieldExpr}_value=${fieldValue}"/>
    </c:forEach>
  </c:if>
  <html:link action="/template?name=${templateQuery.name}&type=${type}${extra}" 
             title="${linkTitle}">
    <img border="0" class="arrow" src="images/right-arrow.gif" alt="->"/>
  </html:link>
  <c:if test="${type == 'user'}">
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
      <c:set target="${deleteParams}" property="confirmAction" value="/userTemplateAction?method=delete&name=${templateQuery.name}&type=${templateType}" />
      <c:set target="${deleteParams}" property="cancelAction" value="/begin" />
    </jsp:useBean>
    <html:link action="/confirm" name="deleteParams" title="${linkTitle}">
      <img border="0" src="images/cross.gif" alt="x"/>
    </html:link>
    <c:remove var="deleteParams"/>
    <fmt:message var="linkTitle" key="templateList.edit">
      <fmt:param value="${templateQuery.name}"/>
    </fmt:message>
    <html:link action="/editTemplate?name=${templateQuery.name}" title="${linkTitle}">
      <img border="0" class="arrow" src="images/edit.gif" alt="->"/>
    </html:link>
  </c:if>
  <c:if test="${!status.last}">
    <div class="seperator"></div>
  </c:if>
</c:forEach>

<c:if test="${empty templates}">
  <div class="altmessage"><fmt:message key="templateList.noTemplates"/></div>
</c:if>


