<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- begin.jsp -->
<div>
  <html:link action="/classChooser">
    <fmt:message key="index.classChooser"/>
  </html:link>
</div>
<c:if test="${!empty PROFILE.savedBags || !empty PROFILE.savedQueries}">
  <div>
    <html:link action="/history">
      <fmt:message key="index.history"/>
    </html:link>
  </div>
</c:if>
<div>
  <html:link action="/bagBuild">
    <fmt:message key="index.bagBuild"/>
  </html:link>
</div>
<div>
  <html:link action="/examples">
    <fmt:message key="index.examples"/>
  </html:link>
</div>
<div>
  <html:link action="/iqlQuery">
    <fmt:message key="index.iqlQuery"/>
  </html:link>
</div>
<!-- /begin.jsp -->
