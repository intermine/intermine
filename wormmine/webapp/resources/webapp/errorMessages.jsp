<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.HashMap, org.apache.struts.action.*"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<!-- errorMessages.jsp -->
<tiles:importAttributes/>
<% 
ActionMessages actionMessage = (ActionMessages) request.getAttribute("org.apache.struts.action.ERROR");
int size = 0;
if (actionMessage != null)
    size = actionMessage.size();
%>
<script type="text/javascript" charset="utf-8">
var haserrors=0;
var haslookup=0;
var hasmessages=0;
var strutsMsgSize = <%=size%>;

<!-- ERRORS -->
<logic:messagesPresent>
    if (strutsMsgSize > 1) {
      jQuery('#error_msg').append('<ul>');
    }
    <html:messages id="error">
    if (strutsMsgSize > 1) {
        jQuery('#error_msg').append('<li><imutil:treatString><c:out value="${error}" escapeXml="false"/></imutil:treatString></li>');
    } else {
        jQuery('#error_msg').append('<imutil:treatString><c:out value="${error}" escapeXml="false"/></imutil:treatString>');
    }
    haserrors=1;
    </html:messages>
    if (strutsMsgSize > 1) {
        jQuery('#error_msg').append('</ul>');
    }
</logic:messagesPresent>

<!-- LOOKUP & PORTAL -->
<logic:messagesPresent name="PORTAL_MSG">
  <html:messages id="message" name="PORTAL_MSG">
      jQuery('#lookup_msg').append('<imutil:treatString><c:out value="${message}" escapeXml="false"/></imutil:treatString>');
      haslookup=1;
  </html:messages>
</logic:messagesPresent>

<logic:messagesPresent name="LOOKUP_MSG">
  <html:messages id="message" name="LOOKUP_MSG">
      jQuery('#lookup_msg').append('<imutil:treatString><c:out value="${message}" escapeXml="false"/></imutil:treatString>');
      haslookup=1;
  </html:messages>
</logic:messagesPresent>

<!-- ERRORS II -->
<c:if test="${!empty ERRORS}">
    <c:forEach items="${ERRORS}" var="error">
      jQuery('#error_msg').append('<imutil:treatString><c:out value="${error}" escapeXml="false"/></imutil:treatString><br/>');
      haserrors=1;
    </c:forEach>
  <c:remove var="ERRORS" scope="session"/>
</c:if>

<!-- MESSAGES -->
<logic:messagesPresent message="true">
    <html:messages id="message" message="true">
      jQuery('#msg').append('<imutil:treatString><c:out value="${message}" escapeXml="false"/></imutil:treatString><br/>');
      hasmessages=1;
    </html:messages>
</logic:messagesPresent>

<!-- MESSAGES II -->
<c:if test="${!empty MESSAGES}">
    <c:forEach items="${MESSAGES}" var="message">
      <c:choose>
        <c:when test="${message == 'You have logged out'}">
            //window.close();
            window.location = "/";
        </c:when>
        <c:otherwise>
          jQuery('#msg').append('<imutil:treatString><c:out value="${message}" escapeXml="false"/></imutil:treatString><br />');
        hasmessages=1;
        </c:otherwise>
      </c:choose>
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
