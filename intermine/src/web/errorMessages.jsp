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

<c:if test="${!empty ERRORS}">
  <div class="errors">
    <c:forEach items="${ERRORS}" var="error">
      <c:out value="${error}"/><br/>
    </c:forEach>
  </div>
  <br/>
  <c:remove var="ERRORS" scope="session"/>
</c:if>

<logic:messagesPresent message="true">
  <div class="messages">
    <html:messages id="message" message="true">
      <c:out value="${message}"/><br/>
    </html:messages>
  </div>
  <br/>
</logic:messagesPresent>

<c:if test="${!empty MESSAGES}">
  <div class="messages">
    <c:forEach items="${MESSAGES}" var="message">
      <c:out value="${message}"/><br/>
    </c:forEach>
  </div>
  <br/>
  <c:remove var="MESSAGES" scope="session"/>
</c:if>


<!-- /errorMessages.jsp -->
