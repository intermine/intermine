<%@ page import="java.util.*" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<html:form action="/query" focus="querystring"
         onsubmit="return validateQueryForm(this);">
<table border="0" width="100%">

  <tr>
    <td align="right">
      <bean:message key="prompt.querystring"/>:
    </td>
    <td align="left">
      <html:textarea property="querystring" rows="4" cols="80"/>
    </td>
  </tr>

  <tr>
    <td align="right">
      <html:submit>
        <bean:message key="button.submit"/>
      </html:submit>
    </td>
    <td align="left">
      <html:reset>
        <bean:message key="button.reset"/>
      </html:reset>
    </td>
  </tr>

</table>

</html:form>

