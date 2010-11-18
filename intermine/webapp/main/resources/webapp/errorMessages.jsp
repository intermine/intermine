<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil" %>

<!-- errorMessages.jsp -->
<tiles:importAttributes/>


<script type="text/javascript" charset="utf-8">

var haserrors=0;
var haslookup=0;
var hasmessages=0;
    

<!-- ERRORS -->
<logic:messagesPresent>
    <html:messages id="error">
      new Insertion.Bottom('error_msg','<imutil:treatString><c:out value="${error}" escapeXml="false"/></imutil:treatString><br />');
      haserrors=1;
    </html:messages>
</logic:messagesPresent>

<!-- LOOKUP & PORTAL -->
<logic:messagesPresent name="PORTAL_MSG">
  <html:messages id="message" name="PORTAL_MSG">
      new Insertion.Bottom('lookup_msg','<imutil:treatString><c:out value="${message}" escapeXml="false"/></imutil:treatString>');
      haslookup=1;
  </html:messages>
</logic:messagesPresent>

<logic:messagesPresent name="LOOKUP_MSG">
  <html:messages id="message" name="LOOKUP_MSG">
      new Insertion.Bottom('lookup_msg','<imutil:treatString><c:out value="${message}" escapeXml="false"/></imutil:treatString>');
      haslookup=1;
  </html:messages>
</logic:messagesPresent>

<!-- ERRORS II -->
<c:if test="${!empty ERRORS}">
    <c:forEach items="${ERRORS}" var="error">
      new Insertion.Bottom('error_msg','<imutil:treatString><c:out value="${error}" escapeXml="false"/></imutil:treatString><br />');
      haserrors=1;
    </c:forEach>
  <c:remove var="ERRORS" scope="session"/>
</c:if>

<!-- MESSAGES -->
<logic:messagesPresent message="true">
    <html:messages id="message" message="true">
      new Insertion.Bottom('msg','<imutil:treatString><c:out value="${message}" escapeXml="false"/></imutil:treatString>');
      hasmessages=1;
    </html:messages>
</logic:messagesPresent>

<!-- MESSAGES II -->
<c:if test="${!empty MESSAGES}">
    <c:forEach items="${MESSAGES}" var="message">
      new Insertion.Bottom('msg','<imutil:treatString><c:out value="${message}" escapeXml="false"/></imutil:treatString><br />');
      hasmessages=1;
    </c:forEach>
  <c:remove var="MESSAGES" scope="session"/>
</c:if>

if(haserrors) {
    jQuery('#error_msg').fadeIn(2000);
}
if(hasmessages) {
    jQuery('#msg').fadeIn(2000);
}
if(haslookup) {
    jQuery('#lookup_msg').fadeIn(2000);
}

</script>


<!-- /errorMessages.jsp -->
