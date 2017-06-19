<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

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
<h3>Header</h3>
<c:forEach var="item" items="${header}">
  <c:out value="${item}"/>
  <br/>
</c:forEach>

