<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- fqlquery.jsp -->
<html:form action="/fqlquery" focus="querystring" onsubmit="return validateFqlQueryForm(this);">
  <table border="0" width="100%">
    <tr>
      <td align="center">
        <fmt:message key="fqlquery.prompt"/>:
      </td>
    </tr>
    <tr>
      <td align="center">
        <html:textarea property="querystring" rows="4" cols="20"/>
      </td>
    </tr>
    
    <tr>
      <td align="center">
        <html:submit property="action">
          <fmt:message key="button.run"/>
        </html:submit>
        <html:submit property="action">
          <fmt:message key="button.view"/>
        </html:submit>
        <html:reset>
        <fmt:message key="button.reset"/>
        </html:reset>
      </td>
    </tr>
  </table>
</html:form>
<!-- /fqlquery.jsp -->