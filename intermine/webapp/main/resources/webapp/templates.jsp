<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<!-- templates.jsp -->
<html:xhtml/>

<c:choose>
  <c:when test="${empty param.category}">
    <c:forEach items="${CATEGORIES}" var="category">
      <div class="heading">${category}</div>
      <div class="body">
        <tiles:insert name="templateList.tile">
          <tiles:put name="scope" value="global"/>
          <tiles:put name="placement" value="${category}"/>
          <tiles:put name="noTemplatesMsgKey" value="templateList.noTemplates"/>
        </tiles:insert>
      </div>
      <im:vspacer height="5"/>
    </c:forEach>
  </c:when>
  <c:otherwise>
    <div class="heading">${param.category}</div>
    <div class="body">
      <tiles:insert name="templateList.tile">
        <tiles:put name="type" value="global"/>
        <tiles:put name="placement" value="${param.category}"/>
        <tiles:put name="noTemplatesMsgKey" value="templateList.noTemplates"/>
      </tiles:insert>
    </div>
  </c:otherwise>
</c:choose>
<div class="body">
  <p>
    <html:link action="/search?type=template">
      <fmt:message key="templates.searchtemplates"/>
    </html:link>
  </p>
</div>
<!-- /templates.jsp -->
