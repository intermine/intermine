<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>

<!-- historyQueryView.jsp -->
<div class="historyView">
  <c:if test="${!empty SAVED_QUERIES}">
    <span class="historyViewTitle">
      <fmt:message key="query.savedqueries.header"/>
    </span>
    <br/><br/>
    <table class="results" cellspacing="0">
      <tr>
        <th align="left">
          <fmt:message key="query.savedqueries.namecolumnheader"/>
        </th>
        <th align="right">
          <fmt:message key="query.savedqueries.countcolumnheader"/>
        </th>
        <th/>
      </tr>
      <c:forEach items="${SAVED_QUERIES}" var="savedQuery">
        <tr>
          <td align="left">
            <html:link action="/loadQuery?queryName=${savedQuery.key}">
              <c:out value="${savedQuery.key}"/>
            </html:link>
          </td>
          <td align="right">
            <c:if test="${savedQuery.value.resultsInfo != null}">
              <c:out value="${savedQuery.value.resultsInfo.rows}"/>
            </c:if>
          </td>
          <td>
            <html:link action="/deleteQuery?name=${savedQuery.key}">delete</html:link>
          </td>
        </tr>
      </c:forEach>
    </table>
  </c:if>
</div>
<!-- /historyQueryView.jsp -->
