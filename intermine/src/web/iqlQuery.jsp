<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>

<!-- iqlQuery.jsp -->
<div class="iqlQueryForm">
  <html:form action="/iqlquery" focus="querystring" onsubmit="return validateIqlQueryForm(this);">
    <fmt:message key="iqlquery.prompt"/>:
    <br/>
    <html:textarea property="querystring" rows="10" cols="80"/>
    <br/>
    <html:submit property="action">
      <fmt:message key="button.run"/>
    </html:submit>
    <%-- html:submit property="action">
      <fmt:message key="button.view"/>
    </html:submit --%>
    <html:reset>
      <fmt:message key="button.reset"/>
    </html:reset>
  </html:form>
</div>
<!-- /iqlQuery.jsp -->
