<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>

<table border=0>
  <tr><td>
      <table border="1">
        <c:forEach var="fromItem" items="${query.from}">
          <tr><td>
              <font class="queryViewFromItemTitle"><c:out value="${fromItem}"/></font>
          </td></tr>
        </c:forEach>
      </table>
    </td>
  </tr>
  <tr><td><c:out value="${query}"/></td></tr>
</table>

