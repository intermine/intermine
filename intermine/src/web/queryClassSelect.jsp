<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>

<tiles:importAttribute/>

<!-- queryClassSelect.jsp -->
<html:form action="/queryClassSelect">
  <html:select property="className" size="20">
    <c:forEach items="${classes}" var="entry">
      <html:option value="${entry.key}">
        <c:out value="${entry.value}"/>
      </html:option>
    </c:forEach>
  </html:select>
  <br/>
  <html:submit property="action">
    <fmt:message key="button.addclass"/>
  </html:submit>
  <html:submit property="action">
    <fmt:message key="button.browse"/>
  </html:submit>
</html:form>
<!-- /queryClassSelect.jsp -->
