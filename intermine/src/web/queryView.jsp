<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>

<table border=0>
  <tr><td>
      <table border="0">
        <% org.flymine.objectstore.query.Query query = (org.flymine.objectstore.query.Query) request.getAttribute("query"); %>
        <% request.setAttribute("pclist", org.flymine.objectstore.query.presentation.ConstraintListCreator.createList(query)); %>
        <% java.util.Iterator iter1 = (query == null ? java.util.Collections.EMPTY_LIST : ((java.util.Collection) query.getFrom())).iterator(); while (iter1.hasNext()) { request.setAttribute("fromElement", iter1.next()); %> <!-- c:forEach var="fromElement" items="${query.from}" -->
          <tr><td>
              <table border="1"><tr><td>
                    <font class="queryViewFromItemTitle">
                      <% org.flymine.objectstore.query.FromElement fromElement = (org.flymine.objectstore.query.FromElement) request.getAttribute("fromElement"); if (fromElement instanceof org.flymine.objectstore.query.QueryClass) { String fromElementString = fromElement.toString() ; out.print(fromElementString.substring(fromElementString.lastIndexOf(".") + 1)); } else { out.print(fromElement.toString()); }; %> <!-- <c:out value="${fromElement}"/> -->
                    </font>
                    "<font class="queryViewFromItemAlias"><%= query.getAliases().get(request.getAttribute("fromElement")) %></font>"
                    <% java.util.Iterator iter2 = ((java.util.Collection) request.getAttribute("pclist")).iterator(); while (iter2.hasNext()) { request.setAttribute("pc", iter2.next()); %> <!-- c:forEach var="pc" items="${pclist}" -->
                      <% if (((org.flymine.objectstore.query.presentation.PrintableConstraint) request.getAttribute("pc")).isAssociatedWith((org.flymine.objectstore.query.FromElement) request.getAttribute("fromElement"))) { request.removeAttribute("forThisFromItem"); } else { request.setAttribute("forThisFromItem", "false"); }; %>
                      <c:if test="${empty forThisFromItem}">
                        <br/>
                        <font class="queryViewConstraintLeft"><c:out value="${pc.left}"/></font> <font class="queryViewConstraintOp"><c:out value="${pc.op}"/></font> <font class="queryViewConstraintRight"><c:out value="${pc.right}"/></font>
                      </c:if>
                    <% } %> <!-- /c:forEach -->
              </td></tr></table>
          </td></tr>
        <% } %> <!-- /c:forEach -->
        <% java.util.Iterator iter3 = ((java.util.Collection) request.getAttribute("pclist")).iterator(); while (iter3.hasNext()) { request.setAttribute("pc", iter3.next()); %> <!-- c:forEach var="pc" items="${pclist}" -->
          <% if (((org.flymine.objectstore.query.presentation.PrintableConstraint) request.getAttribute("pc")).isAssociatedWithNothing()) { request.removeAttribute("forNothing"); } else { request.setAttribute("forNothing", "false"); }; %>
          <c:if test="${empty forNothing}">
            <tr><td>
                Extra: <font class="queryViewConstraintExtra"><c:out value="${pc}"/></font>
            </td></tr>
          </c:if>
        <% } %> <!-- /c:forEach -->
      </table>
    </td>
  </tr>
  <tr><td><c:out value="${query}"/></td></tr>
</table>

