<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>

<!-- errorMessages.jsp -->
<div class="errors">
  <logic:messagesPresent>
    <html:messages id="error">
      <c:out value="${error}"/><br/>
    </html:messages>
  </logic:messagesPresent>
</div>  

<div class="messages">
  <logic:messagesPresent message="true">
    <html:messages id="message" message="true">
      <c:out value="${message}"/><br/>
    </html:messages>
    <br/>
  </logic:messagesPresent>
</div>
<!-- /errorMessages.jsp -->
