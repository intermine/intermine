<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- feedbackForm.jsp -->

<html:xhtml/>
<link rel="stylesheet" type="text/css" href="css/feedbackForm.css"/>
<div class="body">
  <html:form action="/feedbackAction" styleId="feedbackForm">
  <table cellspacing="0" cellpadding="3" border="0">
  <tr>
    <td align="right"><fmt:message key="feedback.name"/></td>
    <td>
      <html:text property="name" size="40" styleId="fbname"/>
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
    <td><html:textarea property="message" cols="80" rows="10" style="width: 100%" styleId="fbcomment"/></td>
  </tr>
  <tr>
    <td align="center"><html:submit/></td>
  </tr>
  </table>
  </html:form>
</div>

<!-- /feedbackForm.jsp -->

