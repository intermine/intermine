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
         <th> Position  </th>
         <th> Alternate </th>
         <th> Reference </th>
         <th> Substitution </th>
         <th> Classification </th>
         <th> Transcript(s) </th>
         <th> Genotype </th>
         <th> Sample Name(s) </th>
       </tr>
    </thead>
    <tbody>
	  <c:forEach var="row" items="${list}">
	     <tr>
           <td rowspan=${row.genoSampleCount} > ${row.position} </td>
           <td rowspan=${row.genoSampleCount} > ${row.alternate} </td>
           <td rowspan=${row.genoSampleCount} > ${row.reference} </td>
           <td rowspan=${row.genoSampleCount} > ${row.substitution} </td>
           <td rowspan=${row.genoSampleCount} > ${row.classification} </td>
           <td rowspan=${row.genoSampleCount} > ${row.transcripts} </td>
           <c:set var="ctr" value="1" scope="page" />
                <c:forEach var="genoSample" items="${row.genoSamples}">
                     <td> ${genoSample.genotype} </td>
                     <td> ${genoSample.samples} </td>
                  <c:if test="${ctr < row.genoSampleCount}">
                    </tr> <tr>
                  </c:if>
                  <c:set var="ctr" value="${ctr + 1}" scope="page" />
                </c:forEach>
	      </tr>
          <!-- whenever we have an even number of genotypes, we    -->
          <!-- want to put in an empty row in order to get the    -->
          <!-- alternating coloring of the rows to be consistent. -->
          <c:if test="${row.genoSampleCount %2 == 0}" >
            <tr></tr>
          </c:if>
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
