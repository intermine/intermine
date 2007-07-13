<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<!-- errorMessages.jsp -->
<link rel="stylesheet" type="text/css" href="css/errorMessages.css"/>

<logic:messagesPresent>
  <div class="topBar errors">
    <html:messages id="error">
      <c:out value="${error}"/><br/>
    </html:messages>
  </div>  
  <br/>
</logic:messagesPresent>

<noscript>
  <div class="topBar errors">
    <fmt:message key="errors.noscript"/>
  </div>
  <br/>
</noscript>

<c:if test="${!empty ERRORS}">
  <div class="topBar errors">
    <c:forEach items="${ERRORS}" var="error">
      <c:out value="${error}"/><br/>
    </c:forEach>
  </div>
  <br/>
  <c:remove var="ERRORS" scope="session"/>
</c:if>

<logic:messagesPresent message="true">
  <div class="topBar messages">
    <html:messages id="message" message="true">
      <c:out value="${message}" escapeXml="false"/><br/>
    </html:messages>
  </div>
  <br/>
</logic:messagesPresent>

<c:if test="${!empty MESSAGES}">
  <div class="topBar messages">
    <c:forEach items="${MESSAGES}" var="message">
      <c:out value="${message}" escapeXml="false"/><br/>
    </c:forEach>
  </div>
  <br/>
  <c:remove var="MESSAGES" scope="session"/>
</c:if>


<!-- /errorMessages.jsp -->
