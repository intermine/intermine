<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>

<!-- begin.jsp -->
<div>
  <html:link action="/classChooser">
    <fmt:message key="index.classChooser"/>
  </html:link>
</div>
<c:if test="${!empty SAVED_BAGS || !empty SAVED_QUERIES}">
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
  <html:link page="/buildiqlquery">
    <fmt:message key="index.iqlQuery"/>
  </html:link>
</div>
<!-- /begin.jsp -->
