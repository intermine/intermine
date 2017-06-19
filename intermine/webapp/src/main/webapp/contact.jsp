<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- feedback.jsp -->

<script type="text/javascript">
<!--
window.onload = function() {
  document.getElementById("fbname").focus();
}
// -->
</script>

<div id="contactFormResponse" style="display:none;">
<c:choose>
  <c:when test="${!empty response}">
  <div style="" id="error_msg" class="topBar errors">
        <a href="#" onclick="jQuery('#contactFormResponse').remove();return false">Hide</a>
      ${response}<br></div>
  </c:when>
  <c:otherwise>
  <div style="" id="error_msg" class="topBar messages">
        <a href="#" onclick="jQuery('#contactFormResponse').remove();return false">Hide</a>
      Thank you for contacting us!<br></div>
  </c:otherwise>
</c:choose>
</div>

<c:if test="${empty sent}">
<tiles:get name="contactForm.jsp"/>
</c:if>

<!-- /feedback.jsp -->