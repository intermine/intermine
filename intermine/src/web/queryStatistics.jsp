<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>

<!-- queryStatistics.jsp -->
<div>
  <c:if test="${QUERY != null}">
    This query (<c:out value="${PROFILE.savedQueries[QUERY]}"/>)
    is estimated to return 
    <c:out value="${QUERY_INFO_MAP[QUERY].resultsInfo.rows + 0}"/> rows and take 
    <c:out value="${QUERY_INFO_MAP[QUERY].resultsInfo.start / 1000}"/> seconds to return
    the first results.
  </c:if>
</div>
<!-- /queryStatistics.jsp -->
