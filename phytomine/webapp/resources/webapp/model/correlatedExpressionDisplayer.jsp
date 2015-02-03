<!-- correlatedExpressionDisplayer.jsp -->
<%@ page language="java" contentType="text/html; charset=US-ASCII"
    pageEncoding="US-ASCII"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<div id="correlated_expression_displayer" class="collection-table">

<h3>Pearson Correlated Expression</h3>

<c:choose>
  <c:when test="${!empty list}">
    <div>
    There are ${fn:length(list)} &nbsp; ${type} with correlated expression &gt; ${threshold}
    <table>
      <thead>
       <tr>
         <th> Gene </th>
         <th> Defline </th>
         <th> Correlation </th>
       </tr>
    </thead>
    <tbody>
      <c:set var="geneList" value="" />
	  <c:forEach var="row" items="${list}">
	    <tr>
         <td> <a href="report.do?id=${row.id}"> ${row.geneName} </a> </td>
         <td> ${row.defline} </td>
         <td> <fmt:formatNumber type="number" maxFractionDigits="5" value="${row.correlation}"/> </td>
         <c:set var="geneList" value="${geneList}${row.geneName} " />
        </tr>
	  </c:forEach>
      </tbody>
    </table>
    <div class="show-in-table" >

    <form method="POST" action="buildBag.do">
      <c:if test="${fn:startsWith(type,'gene')}" >
      <input type="hidden" name="type" value="Gene"/>
      </c:if>
      <c:if test="${fn:startsWith(type,'mrna')}" >
      <input type="hidden" name="type" value="Transcript"/>
      </c:if>
      <input type="hidden" name="text" value="${geneList}" />
      <input type="submit" value="Create a list from results" />
    </form>

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
