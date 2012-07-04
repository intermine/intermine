<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- aspectSelect.jsp -->

<html:xhtml/>

  <select name="name" onchange="changeaspect()" id="aspectSelector">
    <c:if test="${aspect == null}">
      <option value="" selected>-- Choose aspect --</option>
    </c:if>
    <c:forEach items="${ASPECTS}" var="entry">
      <c:set var="set" value="${entry.value}"/>
      <option value="${set.name}"
        <c:if test="${aspect.name == set.name}">
          selected
        </c:if>
      >${set.name}</option>
    </c:forEach>
  </select>


<!-- /aspectSelect.jsp -->
