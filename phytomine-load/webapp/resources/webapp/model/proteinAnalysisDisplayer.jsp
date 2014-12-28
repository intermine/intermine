<!-- proteinAnalysisDisplayer.jsp -->
<%@ page language="java" contentType="text/html; charset=US-ASCII"
    pageEncoding="US-ASCII"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<div id="protein_analysis_displayer" class="collection-table">

<h3>InterPro Analysis Data</h3>

<c:choose>
  <c:when test="${!empty list}">
    <div>
    There are ${fn:length(list)} InterPro results.
    <table>
      <thead>
       <tr>
         <th> Program Name </th>
         <th> Subject  </th>
         <th> Start </th>
         <th> End </th>
         <th> Score </th>
         <th> Significance </th>
       </tr>
    </thead>
    <tbody>
	  <c:forEach var="row" items="${list}">
	     <tr>
	       <c:forEach var="column" items="${row}" varStatus="columnStatus">
	            <td>${column}</td>
	        </c:forEach>
	      </tr>
	    </c:forEach>
      </tbody>
    </table>
    <c:if test="${listSize >= 9}">
      <div class="toggle">
        <a class="more">Show more rows</a>
      </div>
    </c:if>
   </div>
  </c:when>
  <c:otherwise>
    No analysis data available
  </c:otherwise>
</c:choose>


<script type="text/javascript">
        trimTable('#protein_analysis_displayer');
</script>

</div>
