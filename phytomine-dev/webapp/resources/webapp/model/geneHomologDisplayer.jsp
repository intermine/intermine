<!-- geneSNPDisplayer.jsp -->
<%@ page language="java" contentType="text/html; charset=US-ASCII"
    pageEncoding="US-ASCII"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<div id="gene_homolog_displayer" class="collection-table">

<h3>Homologs</h3>

<c:choose>
  <c:when test="${!empty list}">
    <div>
    There are ${fn:length(list)} Homologs for this Gene
    <table>
      <thead>
       <tr>
         <th> Ortholog Group Name</th>
         <th> Gene Name  </th>
         <th> Organism Name  </th>
         <th> Relationship </th>
       </tr>
    </thead>
    <tbody>
      <c:forEach var="row" items="${list}">
        <tr>
           <td> <a href="report.do?id=${row[0]}">${row[1]}</a> </td>
           <td> <a href="report.do?id=${row[2]}">${row[3]}</a> </td>
           <td> ${row[4]} </td>
           <td> ${row[5]} </td>
        </tr>
      </c:forEach>
    </tbody>
    </table>
    </div>
   </div>
  </c:when>
  <c:otherwise>
    No homolog data available
  </c:otherwise>
</c:choose>


<script type="text/javascript">
        numberOfTableRowsToShow=100000
        trimTable('#gene_homolog_displayer');
</script>


</div>
