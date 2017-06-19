<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- listTags.jsp -->
<b>Tags:</b>
  <c:forEach items="${currentTags}" var="item" varStatus="status">
        ${item}&nbsp;
  </c:forEach>
<!-- /listTags.jsp -->

