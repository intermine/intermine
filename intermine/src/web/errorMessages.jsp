<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- errorMessages.jsp -->
<logic:messagesPresent>
  <div class="errors">
    <html:messages id="error">
      <c:out value="${error}"/><br/>
    </html:messages>
  </div>  
  <br/>
</logic:messagesPresent>

<logic:messagesPresent message="true">
  <div class="messages">
    <html:messages id="message" message="true">
      <c:out value="${message}"/><br/>
    </html:messages>
  </div>
  <br/>
</logic:messagesPresent>

<c:if test="${!empty MESSAGE}">
  <div class="messages">
    <c:out value="${MESSAGE}"/>
  </div>
  <br/>
  <c:remove var="MESSAGE" scope="session"/>
</c:if>

<!-- /errorMessages.jsp -->
