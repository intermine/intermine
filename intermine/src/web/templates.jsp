<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- templates.jsp -->
<c:forEach items="${templates}" var="templateQuery">
  <c:out value="${templateQuery.cleanDescription}"/>
  <html:link action="/template?name=${templateQuery.identifier}">
    <img class="arrow" src="images/right-arrow.png" alt="->"/>
  </html:link>
  <br/>
</c:forEach>
<!-- /templates.jsp -->
