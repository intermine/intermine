<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- importTemplates.jsp -->
<html:xhtml/>
<im:body>
  <fmt:message key="importTemplates.message"/><p>
  <html:form action="/importTemplates">
    <table cellspacing="0" cellpadding="3" border="0">
    <tr>
      <td align="right" valign="top"><fmt:message key="importTemplates.xml"/></td>
      <td><html:textarea property="xml" cols="60" rows="20"/></td>
    </tr>
    <c:if test="${IS_SUPERUSER}">
      <tr>
        <td align="right" valign="top"><fmt:message key="importTemplates.overwriting"/></td>
        <td><html:checkbox property="overwriting"/>
      </tr>
    </c:if>
    <tr>
      <td>&nbsp;</td>
      <td align="center">
        <html:submit><fmt:message key="importTemplates.submit"/></html:submit>
      </td>
    </tr>
    </table>
  </html:form>
</im:body>
<!-- /importTemplates.jsp -->
