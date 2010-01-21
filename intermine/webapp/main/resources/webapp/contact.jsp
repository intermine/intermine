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


<c:if test="${empty sent}">
<tiles:get name="contactForm.jsp"/>
</c:if>

<!-- /feedback.jsp -->
