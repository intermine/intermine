<%@ taglib uri="/WEB-INF/struts-bean-el.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<!-- restartQuery.jsp -->
<html:form action="/restartQuery">
  <html:submit property="action">
    <bean:message key="query.reset"/>
  </html:submit>
</html:form>
<!-- /restartQuery.jsp -->
