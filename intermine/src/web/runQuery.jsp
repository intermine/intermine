<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>

<c:if test="${query != null}">
  <html:form action="/runQuery">

    <font size="-1">Run this query</font><br/>
    <html:submit property="action">
        <bean:message key="button.run"/>
    </html:submit>
  </html:form>
</c:if>
