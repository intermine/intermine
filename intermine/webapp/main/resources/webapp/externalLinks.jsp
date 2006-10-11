<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<fmt:setBundle basename="model"/>

<!-- externalLinks.jsp -->

<c:if test="${!empty externalLinkPrefixes}">
  <div class="heading">
    <span style="white-space:nowrap">External links for this object</span>
  </div>
  <div class="body">
    <c:forEach items="${externalLinkPrefixes}" var="entry">
      <div>
        <html:link href="${entry.value}">
          ${entry.key}
        </html:link>
      </div>
    </c:forEach>
  </div>
</c:if>
<!-- /externalLinks.jsp -->
