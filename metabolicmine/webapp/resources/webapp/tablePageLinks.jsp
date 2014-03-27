<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>


<tiles:importAttribute/>

<!-- tablePageLinks -->
<div id="table-page-links">
  <c:choose>
    <c:when test="${!resultsTable.firstPage}">
      <c:if test="${short != 'true'}">
        <div>
        <html:link title="First page" action="/${currentPage}?table=${param.table}&amp;bagName=${bagName}&amp;page=0&amp;size=${resultsTable.pageSize}&amp;trail=${param.trail}">
          <img src="images/icons/first-16.png" alt="First" />
        </html:link>
        </div>
      </c:if>
      <div>
      <html:link title="Previous page" action="/${currentPage}?table=${param.table}&amp;bagName=${bagName}&amp;page=${resultsTable.page-1}&amp;size=${resultsTable.pageSize}&amp;trail=${param.trail}">
        <img src="images/icons/previous-16.png" alt="Previous" />
      </html:link>
      </div>
    </c:when>

    <c:otherwise>
      <c:if test="${short != 'true'}">
        <div><img src="images/icons/first-inactive-16.png" alt="First" /></div>
      </c:if>
      <div><img src="images/icons/previous-inactive-16.png" alt="Previous" /></div>
    </c:otherwise>
  </c:choose>

  <c:choose>
    <c:when test="${!resultsTable.lastPage && (resultsTable.page+2)*resultsTable.pageSize < resultsTable.maxRetrievableIndex}">
      <div>
      <html:link title="Next page" action="/${currentPage}?table=${param.table}&amp;bagName=${bagName}&amp;page=${resultsTable.page+1}&amp;size=${resultsTable.pageSize}&amp;trail=${param.trail}">
        <img src="images/icons/next-16.png" alt="Next" />
      </html:link>
      </div>
    </c:when>
    <c:otherwise>
      <div><img src="images/icons/next-inactive-16.png" alt="Next" /></div>
    </c:otherwise>
  </c:choose>

  <c:if test="${short != 'true'}">
    <c:choose>
      <c:when test="${!resultsTable.lastPage && resultsTable.maxRetrievableIndex > resultsTable.estimatedSize}">
        <div>
        <html:link title="Last page" action="/changeTable?table=${param.table}&amp;method=last&amp;trail=${param.trail}&amp;currentPage=${currentPage}&amp;bagName=${bag.name}">
          <img src="images/icons/last-16.png" alt="Last" />
        </html:link>
        </div>
      </c:when>
      <c:otherwise>
        <div><img src="images/icons/last-inactive-16.png" alt="Last" /></div>
      </c:otherwise>
    </c:choose>
  </c:if>
</div>
<!-- /tablePageLinks.jsp -->
