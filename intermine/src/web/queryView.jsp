<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<tiles:importAttribute/>

<!-- queryView.jsp -->
<table border=0>
  <tr>
    <td>
      <table border="0" bordercolor="yellow" cellpadding="5">
        <c:forEach var="fromElement" items="${query.from}" varStatus="status">
          <c:if test="${status.count % 2 == 1}">
            <c:if test="{status.count > 0}"> </tr> </c:if>
              <tr>
            </c:if>
          </c:if>  
          <td align="left" valign="top">
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
          </td>
        </c:forEach>
        <c:forEach var="pc" items="${noFromConstraints}">
          <tr><td>
              Extra: <font class="queryViewConstraintExtra"><c:out value="${pc}"/></font>
          </td>
        </c:forEach>
        </tr>
      </table>
    </td>
  </tr>
  <tr><td><c:out value="${query}"/></td></tr>
</table>
<!-- /queryView.jsp -->
