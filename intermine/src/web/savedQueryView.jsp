<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>

<div class="savedView">
  <c:if test="${!empty SAVED_QUERIES}">
    <fmt:message key="query.savedqueriesheader"/>
    <ul>
      <c:forEach items="${SAVED_QUERIES}" var="queryName">
        <li>
          <html:link action="/loadQuery?queryName=${queryName.key}">
            <c:out value="${queryName.key}"/>
          </html:link>
        </li>
      </c:forEach>
    </ul>
  </c:if>
</div>
