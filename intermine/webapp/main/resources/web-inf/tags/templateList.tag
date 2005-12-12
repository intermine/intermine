<%@ tag body-content="empty"  %>

<%@ attribute name="category" required="true" %>
<%@ attribute name="type" required="true" %>
<%@ attribute name="className" required="false" %>
<%@ attribute name="displayObject" required="false" type="java.lang.Object" %>
<%@ attribute name="important" required="false" type="java.lang.Boolean" %>

<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<c:if test="${type == 'global' && empty className}">
  <c:set var="templates" value="${CATEGORY_TEMPLATES[category]}"/>
</c:if>
<c:if test="${type == 'global' && !empty className}">
  <c:set var="templates" value="${CLASS_CATEGORY_TEMPLATES[className][category]}"/>
</c:if>
<c:if test="${type == 'user'}">
  <c:set var="templates" value="${PROFILE.categoryTemplates[category]}"/>
</c:if>
<c:if test="${!empty displayObject}">
  <c:set var="interMineObject" value="${displayObject.object}"/>
</c:if>

<c:forEach items="${templates}" var="templateQuery" varStatus="status">
  <%-- filter unimportant templates if necessary --%>
  <c:if test="${!important || templateQuery.important}">
    <c:if test="${!empty displayObject.templateCounts[templateQuery.name] &&
                  displayObject.templateCounts[templateQuery.name] == 0}">
      <c:set var="cssClass" value="nullStrike"/>
    </c:if>
    <span class="${cssClass}">
      <im:templateLine type="${type}" templateQuery="${templateQuery}" className="${className}"
                       interMineObject="${interMineObject}"/>
      <c:if test="${!status.last}">
        <hr class="tmplSeperator"/>
      </c:if>
    </span>
  </c:if>
</c:forEach>

<c:if test="${empty templates}">
  <div class="altmessage"><fmt:message key="templateList.noTemplates"/></div>
</c:if>


