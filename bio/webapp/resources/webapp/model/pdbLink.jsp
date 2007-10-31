<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- pdbLink.jsp -->
<fmt:setBundle basename="model"/>

<c:forEach items="${object.evidence}" var="evidence">
  <c:if test="${evidence.title == 'PDB data - dmel'}">
  <div style="margin-left: 20px">
    <c:set var="sourceTitle" value="PDB"/>
    <c:set var="linkProperty" value="${sourceTitle}.url.prefix"/>
    <html:img src="model/images/${sourceTitle}_logo_small.gif" title="${sourceTitle}" />
    <html:link href="${WEB_PROPERTIES[linkProperty]}${object.identifier}"
               title="${sourceTitle}: ${object.identifier}"
               target="view_window">
      ${sourceTitle}: ${object.identifier}
    </html:link>
  </div>
  </c:if>
</c:forEach>

<!-- /pdbLink.jsp -->
