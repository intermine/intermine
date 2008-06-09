<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!-- news.jsp -->
   <div>
      <h2>News</h2>
      <ol id="news">
      <c:choose>
	      <c:when test="${! empty rssMap and fn:length(rssMap) != 0}">
		      <c:forEach items="${rssMap}" var="rssItem" end="1">
		        <li><strong>${rssItem.title}</strong> ${rssItem.description.value}
		        <br/><em>${rssItem.publishedDate}</em>
		        </li>
		      </c:forEach>
	      </c:when>
	      <c:otherwise>
	         <em>There is no news.</em>
	      </c:otherwise>
      </c:choose>
      </ol>
    </div>
 <!-- /news.jsp -->