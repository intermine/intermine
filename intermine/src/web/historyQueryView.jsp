<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>

<!-- historyQueryView.jsp -->
<div class="historyView">
  <c:if test="${!empty SAVED_QUERIES}">
    <span class="historyViewTitle">
      <fmt:message key="query.savedqueries.header"/>
    </span>
    <table>
      <tr>
        <th align="left">
          <fmt:message key="query.savedqueries.namecolumnheader"/>
        </th>
        <th align="right">
          <fmt:message key="query.savedqueries.countcolumnheader"/>
        </th>
      <c:forEach items="${SAVED_QUERIES}" var="queryName">
        <tr>
          <td align="left">
            <html:link action="/loadQuery?queryName=${queryName.key}">
              <c:out value="${queryName.key}"/>
            </html:link>
          </td>
          <td align="right">
            <c:set var="queryFromSavedQueries" 
                   value="${SAVED_QUERIES[queryName.key]}" scope="page"/>
            <c:if test="${QUERY_INFO_MAP[queryFromSavedQueries].resultsInfo != null}">
              <c:out value="${QUERY_INFO_MAP[queryFromSavedQueries].resultsInfo.rows}"/>
            </c:if>
          </td>
        </tr>
      </c:forEach>
    </table>
  </c:if>
</div>
<!-- /historyQueryView.jsp -->
