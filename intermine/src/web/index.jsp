<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<!-- index.jsp -->
<logic:notPresent name="org.apache.struts.action.MESSAGE" scope="application">
  <font color="red">
    ERROR:  Application resources not loaded -- check servlet container
    logs for error messages.
  </font>
</logic:notPresent>

<h3>
  <fmt:message key="index.heading"/>
</h3>
<ul>
  <li>
    <html:link page="/buildquery.do">
      <fmt:message key="index.query"/>
    </html:link>
  </li>
  <li>
    <html:link page="/buildfqlquery.do">
      <fmt:message key="index.fqlquery"/>
    </html:link>
  </li>
</ul>
<!-- /index.jsp -->
