<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- historyQueryView.jsp -->
<html:xhtml/>
<c:if test="${!empty PROFILE.savedQueries}">
  <html:form action="/modifyQuery">
    <im:heading id="queryHistory">
      <fmt:message key="query.savedqueries.header"/>
    </im:heading>
    <im:body id="queryHistory">
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
            <html:link action="/modifyQueryChange?method=load&amp;name=${savedQuery.key}">
              <c:out value="${savedQuery.key}"/>
            </html:link>
          </td>
          <td align="right">
            <c:if test="${savedQuery.value.info != null}">
              <c:out value="${savedQuery.value.info.rows}"/>
            </c:if>
            &nbsp;
          </td>
        </tr>
      </c:forEach>
    </table>
    <br/>
    <html:submit property="delete">
      <fmt:message key="history.delete"/>
    </html:submit>
    </im:body>
  </html:form>
  <br/>
</c:if>
<!-- /historyQueryView.jsp -->
