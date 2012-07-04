<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- reportDisplayerError.jsp -->

  <div class='collection-table warning'>
    <h3>${displayerName}</h3>
    <p>There was a problem rendering the displayer.</p>
    <c:if test="${!empty exception}">
      <p><a href="#" onclick="jQuery('#${displayerName}_error').slideDown();">show error</a></p>
      <div id="${displayerName}_error" style="display:none;">
        <pre>${exception}</pre>
      </div>
    </c:if>
  </div>

<!-- /reportDisplayerError.jsp -->
