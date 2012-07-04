<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<!-- errorMessagesContainers.jsp -->
<link rel="stylesheet" type="text/css" href="css/errorMessages.css"/>
<div>
    <div class="topBar errors" id="error_msg" style="display:none">
    	<a onclick="javascript:jQuery('#error_msg').hide('slow');return false" href="#">Hide</a>
    </div>
    <!-- the fail class is added on list analysis search results -->
    <div class="topBar messages <c:if test="${not empty param.foundItem}">fail</c:if>" id="msg" style="display:none">
    	<a onclick="javascript:jQuery('#msg').hide('slow');return false" href="#">Hide</a>
    </div>
    <div class="topBar lookupReport" id="lookup_msg" style="display:none">
    	<a onclick="javascript:jQuery('#lookup_msg').hide('slow');return false" href="#">Hide</a>
    </div>

    <noscript>
      <div class="topBar errors">
        <p><fmt:message key="errors.noscript"/></p>
      </div>
      <br/>
    </noscript>

</div>
<!-- /errorMessagesContainers.jsp -->
