<%@ taglib uri="/WEB-INF/struts-bean-el.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<c:if test="${(query != null) || (savedQueries != null)}">
  <html:form action="/restartQuery">
    <html:select property="queryName">
      <c:if test="${query != null}">
        <html:option value="empty" key="query.empty"/>
      </c:if>
      <c:forEach items="${savedQueries}" var="entry">
        <html:option value="${entry.key}"/>
      </c:forEach>
    </html:select>
  
    <html:submit property="action">
      <bean:message key="query.abandon"/>
    </html:submit>
  </html:form>
</c:if>
