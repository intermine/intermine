<%@ page import="java.util.*" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<html:form action="/fqlquery" focus="querystring"
         onsubmit="return validateFqlQueryForm(this);">
<table border="0" width="100%">



  <tr>
    <td align="center">
      <bean:message key="fqlquery.prompt"/>:
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
        <bean:message key="button.run"/>
      </html:submit>
      <html:submit property="action">
        <bean:message key="button.view"/>
      </html:submit>
      <html:reset>
        <bean:message key="button.reset"/>
      </html:reset>
    </td>
  </tr>

</table>

</html:form>
