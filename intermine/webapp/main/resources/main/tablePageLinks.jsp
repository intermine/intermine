
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!-- tablePageLinks -->
  <span class="tablePageLinks">
    <c:choose>
      <c:when test="${!resultsTable.firstPage}">
        <html:link action="/results?table=${param.table}&amp;page=0&amp;size=${resultsTable.pageSize}&amp;trail=${param.trail}">
          <fmt:message key="results.first"/>
        </html:link>
        <html:link action="/results?table=${param.table}&amp;page=${resultsTable.page-1}&amp;size=${resultsTable.pageSize}&amp;trail=${param.trail}">
          <fmt:message key="results.previous"/>
        </html:link>
      </c:when>
      <c:otherwise>
        <fmt:message key="results.first"/>
        <fmt:message key="results.previous"/>
      </c:otherwise>
    </c:choose>
    <c:choose>
      <c:when test="${!resultsTable.lastPage && (resultsTable.page+2)*resultsTable.pageSize < resultsTable.maxRetrievableIndex}">
        <html:link action="/results?table=${param.table}&amp;page=${resultsTable.page+1}&amp;size=${resultsTable.pageSize}&amp;trail=${param.trail}">
          <fmt:message key="results.next"/>
        </html:link>
      </c:when>
      <c:otherwise>
        <fmt:message key="results.next"/>
      </c:otherwise>
    </c:choose>
    <c:choose>
      <c:when test="${!resultsTable.lastPage && resultsTable.maxRetrievableIndex > resultsTable.size}">
        <html:link action="/changeTable?table=${param.table}&amp;method=last&amp;trail=${param.trail}">
          <fmt:message key="results.last"/>
        </html:link>
      </c:when>
      <c:otherwise>
        <fmt:message key="results.last"/>
      </c:otherwise>
    </c:choose>
  </span>
<!-- /tablePageLinks.jsp -->
