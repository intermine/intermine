<%@ tag body-content="empty" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<%
   String[] results = org.intermine.web.logic.WebUtil.getHelpPage(request);
   if (results.length > 0) {
      request.setAttribute("helpPage", results[0]);
      if (results.length > 1 && results[1] != null) {
         request.setAttribute("helpPageSubSection", results[1]);
      }
   }
%>

<c:choose>
  <c:when test="${empty helpPage}">
    <%-- do nothing --%>
  </c:when>
  <c:when test="${empty helpPageSubSection}">
    <im:popupHelp pageName="${helpPage}">?</im:popupHelp>
  </c:when>
  <c:otherwise>
    <im:popupHelp pageName="${helpPage}-${helpPageSubSection}">?</im:popupHelp>
  </c:otherwise>
</c:choose>

