<!-- geneSNPDisplayer.jsp -->
<%@ page language="java" contentType="text/html; charset=US-ASCII"
    pageEncoding="US-ASCII"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<div id="gene_homolog_displayer" class="collection-table">

<h3>Orthologs and Paralogs</h3>

<c:choose>
  <c:when test="${!empty list}">
    <div>
    There are ${fn:length(list)} ortho/paralogs for this gene.
    <c:set var="geneList" value="${geneName}" />
    <table>
      <thead>
       <tr>
         <th>Ortholog Group Name</th>
         <th>Evaluation Node</th>
         <th>Gene Name</th>
         <th>Defline</th>
         <th>Organism Name</th>
         <th>Relationship</th>
       </tr>
    </thead>
    <tbody>
      <c:forEach var="row" items="${list}">
        <tr>
           <td> <a href="report.do?id=${row.id}">${row.groupName}</a> </td>
           <td> ${row.nodeName} </td>
           <td> <a href="report.do?id=${row.geneId}">${row.geneName}</a> </td>
           <td> ${row.geneDefline} </td>
           <td> ${row.organism} </td>
           <td> ${row.relationship} </td>
         <c:set var="geneList" value="${geneList} ${row.geneName}" />
        </tr>
      </c:forEach>
    </tbody>
    </table>
    <div class="show-in-table" >
    <form method="POST" action="buildBag.do">
      <input type="hidden" name="type" value="Gene"/>
      <input type="hidden" name="text" value="${geneList}" />
      <input type="submit" value="Create a list of ortholog/paralog genes" />
    </form>
    </div>

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
