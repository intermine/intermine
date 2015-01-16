<!-- geneSNPDisplayer.jsp -->
<%@ page language="java" contentType="text/html; charset=US-ASCII"
    pageEncoding="US-ASCII"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<div id="correlated_expression_displayer" class="collection-table">

<h3>Correlated Expression</h3>

<c:choose>
  <c:when test="${!empty list}">
    <div>
    There are ${fn:length(list)} items with correlated expression &gt; ${threshold}
    <table>
      <thead>
       <tr>
         <th> Gene </th>
         <th> Correlation </th>
       </tr>
    </thead>
    <tbody>
	  <c:forEach var="row" items="${list}">
	    <tr>
         <td> <a href="report.do?id=${row.id}"> ${row.geneName} </a> </td>
         <td> ${row.correlation} </td>
        </tr>
	  </c:forEach>
      </tbody>
    </table>
    <!-- this will not work until I name the collection -->
    <!-- div class="show-in-table" style="display:none;" -->
    <!-- html:link action="/collectionDetails?id=${id}&amp;field=snps&amp;trail=${param.trail}" -->
    <!-- Show all in a table -->
    <!-- /html:link -->
    </div>
   </div>
  </c:when>
  <c:otherwise>
    No expression correlation data available.
  </c:otherwise>
</c:choose>

<script type="text/javascript">
        numberOfTableRowsToShow=100000
        trimTable('#correlated_expression_displayer');
</script>

</div>
