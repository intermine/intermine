<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- importTemplates.jsp -->
<html:xhtml/>

<div class="body" align="center">
<div class="actionArea" style="width:60%;clear:both;text-align:left">

<p><fmt:message key="importTemplates.intro"/></p>

<br/>

<p>
  <html:form action="/importTemplates">
	<div align="center">
    <table cellspacing="0" cellpadding="0" border="0">
    <tr>
      <td align="center"><html:textarea property="xml" cols="60" rows="20"/></td>
    </tr>
    <c:if test="${IS_SUPERUSER}">
      <tr>
        <td align="right" valign="top"><fmt:message key="importTemplates.overwriting"/> <html:checkbox property="overwriting"/></td>
      </tr>
    </c:if>
    <tr>
      <td align="center">
	      <br/>
        <html:submit><fmt:message key="importTemplates.submit"/></html:submit>
      </td>
    </tr>
    </table>
    </div>
  </html:form>
  
  <br/>
  <br/>
  
  <font size="0.8em"><fmt:message key="importTemplates.message"/></font>
  
  
</div></div>
<!-- /importTemplates.jsp -->
