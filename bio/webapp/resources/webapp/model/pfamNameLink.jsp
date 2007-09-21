<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- pfamNameLink.jsp -->
<fmt:setBundle basename="model"/>

<c:if test="${!empty object.name}">
  <div style="margin-left: 20px">
    <c:set var="sourceTitle" value="Pfam"/>
    <c:set var="linkProperty" value="${sourceTitle}.url.prefix"/>
    <html:img src="model/images/${sourceTitle}_logo_small.png" title="${sourceTitle}" />
    <html:link href="${WEB_PROPERTIES[linkProperty]}${object.name}"
               title="${sourceTitle}: ${object.name}"
               target="view_window">
      ${sourceTitle}: ${object.name}
    </html:link>
  </div>
</c:if>
<!-- /pfamNameLink.jsp -->
