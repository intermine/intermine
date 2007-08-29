<%@ tag body-content="empty" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<%
   String[] results = org.intermine.web.logic.TagUtil.getHelpPage(request);
   if (results.length > 0) {
      request.setAttribute("helpPage", results[0]);
      if (results.length > 1 && results[1] != null) {
         request.setAttribute("anchor", results[1]);
      }
   }
%>

<c:choose>
  <c:when test="${empty helpPage}">
    <%-- do nothing --%>
  </c:when>
  <c:when test="${empty anchor}">
    <im:popupHelp pageName="${helpPage}" text="help"/>
  </c:when>
  <c:otherwise>
    <im:popupHelp pageName="${helpPage}" anchor="${anchor}" text="help"/>
  </c:otherwise>
</c:choose>

