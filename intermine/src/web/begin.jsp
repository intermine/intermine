<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>

<!-- begin.jsp -->
<div>
  <html:link action="/classChooser">
    <fmt:message key="index.classchooser"/>
  </html:link>
</div>
<div>
  <html:link action="/history">
    <fmt:message key="index.history"/>
  </html:link>
</div>
<div>
  <html:link action="/bagBuilder">
    <fmt:message key="index.bagbuilder"/>
  </html:link>
</div>
<div>
  <html:link action="/examples">
    <fmt:message key="index.examples"/>
  </html:link>
</div>
<div>
  <html:link page="/buildiqlquery.do">
    <fmt:message key="index.iqlquery"/>
  </html:link>
</div>
<!-- /begin.jsp -->
