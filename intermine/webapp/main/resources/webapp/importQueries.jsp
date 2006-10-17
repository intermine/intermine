<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- importQueries.jsp -->
<html:xhtml/>
<im:body>
  <fmt:message key="importQuery.message"/><p>
  <html:form action="/importQueriesAction?query_build=${param.query_builder}">
    <html:hidden property="query_builder" value="${param.query_builder}"/>
    <table cellspacing="0" cellpadding="3" border="0">
    <tr>
      <td align="right" valign="top"><fmt:message key="importQuery.xml"/></td>
      <td><html:textarea property="xml" cols="60" rows="20"/></td>
    </tr>
    <tr>
      <td>&nbsp;</td>
      <td align="center">
        <html:submit><fmt:message key="importQuery.submit"/></html:submit>
      </td>
    </tr>
    </table>
  </html:form>
</im:body>
<!-- /importQueries.jsp -->
