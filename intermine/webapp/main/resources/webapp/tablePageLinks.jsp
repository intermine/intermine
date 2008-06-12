<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>


<tiles:importAttribute/>

<!-- tablePageLinks -->
      <c:choose>
      <c:when test="${!resultsTable.firstPage}">
        <c:if test="${short != 'true'}">
          <html:link action="/${currentPage}?table=${param.table}&amp;bagName=${bagName}&amp;page=0&amp;size=${resultsTable.pageSize}&amp;trail=${param.trail}">
            &lt;&lt;&nbsp;<fmt:message key="results.first"/>
          </html:link>
        </c:if>
        <html:link action="/${currentPage}?table=${param.table}&amp;bagName=${bagName}&amp;page=${resultsTable.page-1}&amp;size=${resultsTable.pageSize}&amp;trail=${param.trail}">
          &lt;&nbsp;<fmt:message key="results.previous"/>
        </html:link>&nbsp;|&nbsp;
      </c:when>
      <c:otherwise>
        <c:if test="${short != 'true'}">&lt;&lt;&nbsp;<fmt:message key="results.first"/></c:if>
        &lt;&nbsp;<fmt:message key="results.previous"/>&nbsp;|&nbsp;
      </c:otherwise>
    </c:choose>
    <c:choose>
      <c:when test="${!resultsTable.lastPage && (resultsTable.page+2)*resultsTable.pageSize < resultsTable.maxRetrievableIndex}">
        <html:link action="/${currentPage}?table=${param.table}&amp;bagName=${bagName}&amp;page=${resultsTable.page+1}&amp;size=${resultsTable.pageSize}&amp;trail=${param.trail}">
          <fmt:message key="results.next"/>&nbsp;&gt;
        </html:link>
      </c:when>
      <c:otherwise>
        <fmt:message key="results.next"/>&nbsp;&gt;
      </c:otherwise>
    </c:choose>
    <c:if test="${short != 'true'}">
    <c:choose>
      <c:when test="${!resultsTable.lastPage && resultsTable.maxRetrievableIndex > resultsTable.estimatedSize}">
        <html:link action="/changeTable?table=${param.table}&amp;method=last&amp;trail=${param.trail}&amp;currentPage=${currentPage}&amp;bagName=${bag.name}">
          <fmt:message key="results.last"/>&nbsp;&gt;&gt;
        </html:link>
      </c:when>
      <c:otherwise>
        <fmt:message key="results.last"/>&nbsp;&gt;&gt;
      </c:otherwise>
    </c:choose>
    </c:if>
<!-- /tablePageLinks.jsp -->
