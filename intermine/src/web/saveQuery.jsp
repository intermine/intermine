<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>

<c:if test="${query != null}">
  <html:form action="/saveQuery">
    <html:text property="queryName"/>
    <html:submit property="action">
      <bean:message key="query.new"/>
    </html:submit>
  </html:form>
</c:if>
