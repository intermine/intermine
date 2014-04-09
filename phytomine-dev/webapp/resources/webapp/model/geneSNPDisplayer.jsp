<!-- geneSNPDisplayer.jsp -->
<%@ page language="java" contentType="text/html; charset=US-ASCII"
    pageEncoding="US-ASCII"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<div id="gene_snp_displayer" class="collection-table">

<h3>SNV Data</h3>

<c:choose>
  <c:when test="${!empty list}">
    <div>
    There are ${fn:length(list)} SNPs Associated with the Gene
    <table>
      <thead>
       <tr>
         <th> Sample Name(s) </th>
         <th> Position  </th>
         <th> Reference </th>
         <th> Alternate </th>
         <th> Substitution </th>
         <th> Classification </th>
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
    <!-- this will not work until I name the collection -->
    <!-- div class="show-in-table" style="display:none;" -->
    <!-- html:link action="/collectionDetails?id=${id}&amp;field=snps&amp;trail=${param.trail}" -->
    <!-- Show all in a table -->
    <!-- /html:link -->
    </div>
   </div>
  </c:when>
  <c:otherwise>
    No SNP data available
  </c:otherwise>
</c:choose>


<script type="text/javascript">
        numberOfTableRowsToShow=100000
        trimTable('#gene_snp_displayer');
</script>


</div>
