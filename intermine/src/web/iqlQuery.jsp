<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>

<!-- fqlquery.jsp -->
<div class="fqlQueryForm">
  <html:form action="/fqlquery" focus="querystring" onsubmit="return validateFqlQueryForm(this);">
    <fmt:message key="fqlquery.prompt"/>:
    <br/>
    <html:textarea property="querystring" value="${QUERY}" rows="10" cols="80"/>
    <br/>
    <html:submit property="action">
      <fmt:message key="button.run"/>
    </html:submit>
    <html:submit property="action">
      <fmt:message key="button.view"/>
    </html:submit>
    <html:reset>
      <fmt:message key="button.reset"/>
    </html:reset>
  </html:form>
</div>
<!-- /fqlquery.jsp -->
