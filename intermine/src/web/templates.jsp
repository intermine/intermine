<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>

<!-- templates.jsp -->
<c:forEach items="${TEMPLATE_QUERIES}" var="templateQuery">
  <c:out value="${templateQuery.value.cleanDescription}"/>
  <html:link action="/template?name=${templateQuery.key}">
    <img class="arrow" src="images/right-arrow.png" alt="->"/>
  </html:link>
  <br/><br/>
</c:forEach>
<!-- /templates.jsp -->
