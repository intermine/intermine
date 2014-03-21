<!-- familyAlignmentDisplayer.jsp -->
<%@ page language="java" contentType="text/html; charset=US-ASCII"
    pageEncoding="US-ASCII"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<div id="gene-to-snp-displayer" class="collection-table">

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
    <c:if test="${listSize >= 9}">
      <div class="toggle">
        <a class="more">Show more rows</a>
      </div>
    </c:if>
   </div>
  </c:when>
  <c:otherwise>
    No SNP data available
  </c:otherwise>
</c:choose>


<script type="text/javascript">
(function() {
  jQuery("#gene-to-snp-displayer div.toggle a.more").click(function() {
    jQuery("#gene-to-snp-displayer div.locations-table tr:hidden").each(function(index) {
        if (index < 3) {
            jQuery(this).show();
        }
    });
    if (jQuery("#gene-to-snp-displayer div.locations-table tr:hidden").length <= 0) {
        jQuery("#gene-to-snp-displayer div.toggle a.more").remove();
    }
  });

  <%-- fixup number of columns --%>
  var l = jQuery('#gene-to-snp-displayer table tr:first td').length,
       m = jQuery('#gene-to-snp-displayer table tr:last td').length;
  if (l != m) {
    jQuery('#gene-to-snp-displayer table tr:last td:last').attr('colspan', l - m + 1);
  }
})();
</script>

</div>
