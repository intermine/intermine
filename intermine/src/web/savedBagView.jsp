<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>

<!-- savedBagView.jsp -->
<div class="savedView">
  <c:if test="${!empty SAVED_BAGS}">
    <span class="savedViewTitle">
      <fmt:message key="query.savedbags.header"/>
    </span>
    <table>
      <th align="left">
        <fmt:message key="query.savedbags.namecolumnheader"/>
      </th>
      <c:forEach items="${SAVED_BAGS}" var="bagName">
        <tr align="left">
          <td>
            <c:out value="${bagName.key}"/>
          </td>
        </tr>
      </c:forEach>
    </table>
  </c:if>
</div>
<!-- /savedBagView.jsp -->
