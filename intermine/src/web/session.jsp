<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="/WEB-INF/struts-bean-el.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>

<h3>Application</h3>
<c:forEach var="item" items="${applicationScope}">
  <c:out value="${item}"/>
  <br/>
</c:forEach>
<h3>Session</h3>
<c:forEach var="item" items="${sessionScope}">
  <c:out value="${item}"/>
  <br/>
</c:forEach>
<h3>Request</h3>
<c:forEach var="item" items="${requestScope}">
  <c:out value="${item}"/>
  <br/>
</c:forEach>
<h3>Request parameters</h3>
<c:forEach var="item" items="${param}">
  <c:out value="${item}"/>
  <br/>
</c:forEach>
<h3>Page</h3>
<c:forEach var="item" items="${pageScope}">
  <c:out value="${item}"/>
  <br/>
</c:forEach>
