<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- importQueries.jsp -->
<html:xhtml/>
<div class="body" align="center">
<im:boxarea stylename="plainbox" fixedWidth="60%">
  
<p><fmt:message key="importQuery.intro"/></p>

<br/>

<p>
  <html:form action="/importQueriesAction?query_build=${param.query_builder}">
  
    <html:hidden property="query_builder" value="${param.query_builder}"/>
    
    <div align="center">
    <table cellspacing="0" cellpadding="0" border="0">
    <tr>
      <td><html:textarea property="xml" cols="60" rows="20"/></td>
    </tr>
    <tr>
      <td align="center">
      <br/>
        <html:submit><fmt:message key="importQuery.submit"/></html:submit>
      </td>
    </tr>
    </table>
    </div>
  </html:form>
  
  <br/>
  <br/>
  
  <p><font size="0.8em"><fmt:message key="importQuery.message"/></font></p>
</im:boxarea>
</div>
<!-- /importQueries.jsp -->
