<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- iqlQuery.jsp -->
<html:xhtml/>
<div class="iqlQueryForm">
  <html:form action="/iqlQueryAction">
    <fmt:message key="iqlquery.prompt"/>:
    <br/>
    <html:textarea property="querystring" rows="10" cols="80"/>
    <br/>
    <html:submit property="action">
      <fmt:message key="button.run"/>
    </html:submit>
    <html:reset>
      <fmt:message key="button.reset"/>
    </html:reset>
  </html:form>
</div>
<!-- /iqlQuery.jsp -->
