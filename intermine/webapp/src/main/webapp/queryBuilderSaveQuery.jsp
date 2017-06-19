<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- saveQuery.jsp -->
<html:xhtml/>
<c:if test="${!empty QUERY}">
  <c:if test="${PROFILE.loggedIn}">
    <p><html:form action="/saveQuery">
      <html:text property="queryName"/>&nbsp;<html:submit
      property="action">
        <fmt:message key="query.save"/>
      </html:submit>
    </html:form></p>
  </c:if>
</c:if>
<!-- /saveQuery.jsp -->
