<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- saveQuery.jsp -->
<html:xhtml/>
<c:if test="${!empty QUERY}">
  <c:if test="${!empty PROFILE.username}">
  
  <fmt:message key="query.save.msg"/>
    <html:form action="/saveQuery">
      <html:text property="queryName"/>
      <html:submit property="action">
        <fmt:message key="query.save"/>
      </html:submit>
    </html:form>
  </c:if>
  <div class="exportQueryLink">
    <fmt:message key="query.export.as"/>
    <html:link action="/exportQuery?as=xml">
      XML
    </html:link>
  </div>
  <div class="exportQueryLink">
    <html:link action="/exportQuery?as=link&serviceFormat=tab">
        Get the url  
    </html:link> to run this query with a web service to get results as tab separated values. Modify url to get more results.
  </div>
</c:if>
<!-- /saveQuery.jsp -->
