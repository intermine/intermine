
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!-- tablePageLinks -->
  <span class="tablePageLinks">
    <c:choose>
      <c:when test="${!RESULTS_TABLE.firstPage}">
        <html:link action="/results?page=0&amp;size=${RESULTS_TABLE.pageSize}&amp;trail=${param.trail}">
          <fmt:message key="results.first"/>
        </html:link>
        <html:link action="/results?page=${RESULTS_TABLE.page-1}&amp;size=${RESULTS_TABLE.pageSize}&amp;trail=${param.trail}">
          <fmt:message key="results.previous"/>
        </html:link>
      </c:when>
      <c:otherwise>
        <fmt:message key="results.first"/>
        <fmt:message key="results.previous"/>
      </c:otherwise>
    </c:choose>
    <c:choose>
      <c:when test="${!RESULTS_TABLE.lastPage}">
        <html:link action="/results?page=${RESULTS_TABLE.page+1}&amp;size=${RESULTS_TABLE.pageSize}&amp;trail=${param.trail}">
          <fmt:message key="results.next"/>
        </html:link>
      </c:when>
      <c:otherwise>
        <fmt:message key="results.next"/>
      </c:otherwise>
    </c:choose>
    <c:choose>
      <c:when test="${!RESULTS_TABLE.lastPage && RESULTS_TABLE.maxRetrievableIndex > RESULTS_TABLE.size}">
        <html:link action="/changeResults?method=last&amp;trail=${param.trail}">
          <fmt:message key="results.last"/>
        </html:link>
      </c:when>
      <c:otherwise>
        <fmt:message key="results.last"/>
      </c:otherwise>
    </c:choose>
  </span>
<!-- /tablePageLinks.jsp -->
