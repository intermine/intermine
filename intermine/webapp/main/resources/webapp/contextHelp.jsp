<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- contextHelp.jsp -->
<div id="ctxHelpDiv"
  <c:if test="${empty param['ctxHelpTxt']}">
    style="display:none"
  </c:if>
>

  <div class="topBar info">
    <a href="#" onclick="javascript:jQuery('#ctxHelpDiv').hide('slow');return false">Close</a>
    <div id="ctxHelpTxt">${param['ctxHelpTxt']}</div>
  </div>

</div>