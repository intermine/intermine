<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<tiles:importAttribute/>

<table border=0>
  <tr><td>
      <table border="0">
        <c:forEach var="fromElement" items="${query.from}">
          <tr><td>
              <table border="1"><tr><td>
                    <font class="queryViewFromItemTitle">
                      <c:out value="${perFromTitle[fromElement]}"/>
                    </font>
                    "<font class="queryViewFromItemAlias"><c:out value="${perFromAlias[fromElement]}"/></font>"
                    <c:forEach var="pc" items="${perFromConstraints[fromElement]}">
                      <br/>
                      <font class="queryViewConstraintLeft"><c:out value="${pc.left}"/></font> <font class="queryViewConstraintOp"><c:out value="${pc.op}"/></font> <font class="queryViewConstraintRight"><c:out value="${pc.right}"/></font>
                    </c:forEach>
              </td></tr></table>
          </td></tr>
        </c:forEach>
        <c:forEach var="pc" items="${noFromConstraints}">
          <tr><td>
              Extra: <font class="queryViewConstraintExtra"><c:out value="${pc}"/></font>
          </td></tr>
        </c:forEach>
      </table>
    </td>
  </tr>
  <tr><td><c:out value="${query}"/></td></tr>
</table>
