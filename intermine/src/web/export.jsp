<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>

<!-- export.jsp -->
<div>
  <html:link action="/exportAction?method=excel"><fmt:message key="export.excel"/></html:link>
</div>
<div>
  <html:link action="/exportAction?method=csv"><fmt:message key="export.csv"/></html:link>
</div>
<div>
  <html:link action="/exportAction?method=tab"><fmt:message key="export.tabdelimited"/></html:link>
</div>
<!-- /export.jsp -->
