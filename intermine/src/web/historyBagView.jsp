<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>

<!-- historyBagView.jsp -->
<div class="historyView">
  <c:if test="${!empty SAVED_BAGS}">
    <span class="historyViewTitle">
      <fmt:message key="query.savedbags.header"/>
    </span>
    <table>
      <th align="left">
        <fmt:message key="query.savedbags.namecolumnheader"/>
      </th>
      <th align="right">
        <fmt:message key="query.savedbags.countcolumnheader"/>
      </th>
      <c:forEach items="${SAVED_BAGS}" var="bagName">
        <tr align="left">
          <td>
            <html:link action="/bagDetails?bagName=${bagName.key}">
              <c:out value="${bagName.key}"/>
            </html:link>
          </td>
          <td align="right">
            <c:out value="${SAVED_BAGS[bagName.key].size}"/>
          </td>
        </tr>
      </c:forEach>
    </table>
  </c:if>
</div>
<!-- /historyBagView.jsp -->
