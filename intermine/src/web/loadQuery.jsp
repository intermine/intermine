<%@ taglib uri="/WEB-INF/struts-bean-el.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<!-- loadQuery.jsp -->
<c:if test="${!empty savedQueries}">
  <html:form action="/loadQuery">
    <html:select property="queryName">
      <c:forEach items="${savedQueries}" var="entry">
        <html:option value="${entry.key}"/>
      </c:forEach>
    </html:select>
    <html:submit property="action">
      <bean:message key="query.load"/>
    </html:submit>
    <br/>
  </html:form>
</c:if>
<!-- /loadQuery.jsp -->
