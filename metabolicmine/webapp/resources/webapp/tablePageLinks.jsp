<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>


<tiles:importAttribute/>

<!-- tablePageLinks -->
<span class="tablePageLinks">
      <c:choose>
      <c:when test="${!resultsTable.firstPage}">
        <c:if test="${short != 'true'}">
          <html:link action="/${currentPage}?table=${param.table}&amp;bagName=${bagName}&amp;page=0&amp;size=${resultsTable.pageSize}&amp;trail=${param.trail}">
			<img src="images/icons/first-16.png" alt="First" />
          </html:link>
        </c:if>
        <html:link action="/${currentPage}?table=${param.table}&amp;bagName=${bagName}&amp;page=${resultsTable.page-1}&amp;size=${resultsTable.pageSize}&amp;trail=${param.trail}">
			<img src="images/icons/previous-16.png" alt="Previous" />
        </html:link>
      </c:when>
      <c:otherwise>
        <span style="float:left"><c:if test="${short != 'true'}">
        	<img src="images/icons/first-16.png" alt="First" />
	        </c:if>
	        <img src="images/icons/previous-16.png" alt="Previous" />
        </span>
      </c:otherwise>
    </c:choose>
    <c:choose>
      <c:when test="${!resultsTable.lastPage && (resultsTable.page+2)*resultsTable.pageSize < resultsTable.maxRetrievableIndex}">
        <html:link action="/${currentPage}?table=${param.table}&amp;bagName=${bagName}&amp;page=${resultsTable.page+1}&amp;size=${resultsTable.pageSize}&amp;trail=${param.trail}">
          <img src="images/icons/next-16.png" alt="Next" />
        </html:link>
      </c:when>
      <c:otherwise>
        <img src="images/icons/next-16.png" alt="Next" />
      </c:otherwise>
    </c:choose>
    <c:if test="${short != 'true'}">
    <c:choose>
      <c:when test="${!resultsTable.lastPage && resultsTable.maxRetrievableIndex > resultsTable.estimatedSize}">
        <html:link action="/changeTable?table=${param.table}&amp;method=last&amp;trail=${param.trail}&amp;currentPage=${currentPage}&amp;bagName=${bag.name}">
          <img src="images/icons/last-16.png" alt="Last" />
        </html:link>
      </c:when>
      <c:otherwise>
        <img src="images/icons/last-16.png" alt="Last" />
      </c:otherwise>
    </c:choose>
    </c:if>
</span>
<!-- /tablePageLinks.jsp -->
