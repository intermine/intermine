<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- historyQueryView.jsp -->
<c:if test="${!empty PROFILE.savedQueries}">
  <html:form action="/modifyQuery">
    <div class="heading">
      <fmt:message key="query.savedqueries.header"/>
    </div>
    <div class="body">
    <table class="results" cellspacing="0">
      <tr>
        <th>
          &nbsp;
        </th>
        <th align="left">
          <fmt:message key="query.savedqueries.namecolumnheader"/>
        </th>
        <th align="right">
          <fmt:message key="query.savedqueries.countcolumnheader"/>
        </th>
      </tr>
      <c:forEach items="${PROFILE.savedQueries}" var="savedQuery">
        <tr>
          <td>
            <html:multibox property="selectedQueries">
              <c:out value="${savedQuery.key}"/>
            </html:multibox>
          </td>
          <td align="left">
            <html:link action="/modifyQueryChange?method=load&name=${savedQuery.key}">
              <c:out value="${savedQuery.key}"/>
            </html:link>
          </td>
          <td align="right">
            <c:if test="${savedQuery.value.info != null}">
              <c:out value="${savedQuery.value.info.rows}"/>
            </c:if>
          </td>
        </tr>
      </c:forEach>
    </table>
    <br/>
    <html:submit property="delete">
      <fmt:message key="history.delete"/>
    </html:submit>
    </div>
  </html:form>
</c:if>
<!-- /historyQueryView.jsp -->
