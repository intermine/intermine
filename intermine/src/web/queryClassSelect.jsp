<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<tiles:importAttribute/>

<!-- queryClassSelect.jsp -->
<html:xhtml/>
<div class="body">
  <html:form action="/queryClassSelect">
    <html:select property="className" size="20">
      <c:forEach items="${classes}" var="entry">
        <c:if test="${classCounts[entry.key] > 0}">
          <html:option value="${entry.key}">
            <c:out value="${entry.value}"/>
          </html:option>
        </c:if>
      </c:forEach>
    </html:select>
    <br/>
    <html:submit>
      <fmt:message key="button.selectClass"/>
    </html:submit>
  </html:form>
</div>
<!-- /queryClassSelect.jsp -->
