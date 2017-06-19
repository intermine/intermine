<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- importTags.jsp -->
<html:xhtml/>
<div class="body" align="center">
<im:boxarea stylename="plainbox" fixedWidth="60%">
  <p><fmt:message key="importTags.message"/></p>
  <br/>
  <html:form action="/importTags">
     <div align="center">
    <table cellspacing="0" cellpadding="0" border="0">
    <tr>
      <td align="right" valign="top"><fmt:message key="importTags.xml"/></td>
      <td><html:textarea property="xml" cols="60" rows="20"/></td>
    </tr>
     <c:if test="${IS_SUPERUSER}">
      <tr>
        <td colspan="2" align="right" valign="top"><fmt:message key="importTags.overwriting"/> <html:checkbox property="overwriting"/></td>
      </tr>
    </c:if>
    <tr>
      <td colspan="2" align="center">
        <br/>
        <html:submit><fmt:message key="importTags.submit"/></html:submit>
      </td>
    </tr>
    </table>
    </div>
  </html:form>
</im:boxarea>
</div>
<!-- /importTags -->
