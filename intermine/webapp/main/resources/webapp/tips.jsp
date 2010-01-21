<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- tips.jsp -->
<c:set var='n' value='<%= request.getParameter("n") %>' />
<div class="body">
<c:forEach begin="1" end="${n}" var="tipno"> 
  <p>
    <jsp:include page="tips/tip${tipno}_short.jsp"/><br/>
    <a href="/${WEB_PROPERTIES['webapp.path']}/tip.do?id=${tipno}" target="_top">Read more &gt;&gt;</a>
  </p>
</c:forEach>
</div>
<!-- /tips.jsp -->
