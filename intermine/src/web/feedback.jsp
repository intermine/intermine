<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<!-- feedback.jsp -->
<html:xhtml/>

<div class="body">
  <html:form action="/feedbackAction">
  <table cellspacing="0" cellpadding="3" border="0">
  <tr>
    <td align="right"><fmt:message key="feedback.name"/></td>
    <td>
      <html:text property="name" size="40"/>
    </td>
  </tr>
  <tr>
    <td align="right"><fmt:message key="feedback.email"/></td>
    <td>
      <html:text property="email" size="40"/>
    </td>
  </tr>
  <tr>
    <td align="right"><fmt:message key="feedback.subject"/></td>
    <td><html:text property="subject" size="40"/></td>
  </tr>
  <tr>
    <td align="right" valign="top"><fmt:message key="feedback.message"/></td>
    <td><html:textarea property="message" cols="60" rows="20"/></td>
  </tr>
  <tr>
    <td>&nbsp;</td><td align="center"><html:submit/></td>
  </tr>
  </table>
  </html:form>
</div>

<!-- /feedback.jsp -->
