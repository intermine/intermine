<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<tiles:importAttribute/>

<%-- The following should probably be turned into a tag at some stage --%>
<table border="1px" width="90%">
  <%-- The headers --%>
  <tr>
    <c:forEach var="column" items="${query.select}" varStatus="status">
      <c:if test="${resultsTable.columns[status.index].visible}">
        <th align="center" class="resultsHeader">
          <c:out value="${query.aliases[column]}"/>
        </th>
      </c:if>
    </c:forEach>
  </tr>

  <%-- The data --%>

  <%-- Row --%>
  <c:forEach var="row" items="${results}" varStatus="status" begin="${resultsTable.start}" end="${resultsTable.end}">

    <c:set var="rowClass">
      <c:choose>
        <c:when test="${status.count % 2 == 1}">resultsOddRow</c:when>
        <c:otherwise>resultsEvenRow</c:otherwise>
      </c:choose>
    </c:set>

    <tr class="<c:out value="${rowClass}"/>">
      <c:forEach var="item" items="${row}">
        <td>
          <c:out value="${item}"/>
        </td>
      </c:forEach>
    </tr>
  </c:forEach>

</table>
Displaying <c:out value="${resultsTable.start+1}"/> to <c:out value="${resultsTable.end+1}"/> of <c:out value="${resultsTable.size}"/> items.

