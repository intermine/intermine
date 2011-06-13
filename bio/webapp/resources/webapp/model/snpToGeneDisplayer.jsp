<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<html:xhtml/>

  <div id="snp-to-gene-displayer" class="collection-table">
    <h3>SNPs to overlapping Genes within 10.0kb</h3>

    <table>
      <thead>
	      <tr>
	        <th>Gene Primary Identifier</th>
	        <th>Gene name</th>
	        <th>Gene Symbol</th>
	        <th colspan="2">Relative to Gene</th>
	      </tr>
      </thead>
      <tbody>
	      <c:forEach var="row" items="${list}">
	        <tr>
	          <c:forEach var="column" items="${row}" varStatus="columnStatus">
	            <c:choose>
	              <%-- primaryIdentifier & internalID --%>
	              <c:when test="${columnStatus.count == 1}">
	                <td><a title="Go to Gene page" href="report.do?id=${column}">
	              </c:when>
	              <%-- primaryIdentifier & internalID (cont...) --%>
	              <c:when test="${columnStatus.count == 2}">
	                ${column}</a></td>
	              </c:when>

	              <%-- distance --%>
	              <c:when test="${columnStatus.count == 5}">
	                <td class="distance">${column}</td>
	              </c:when>
	              <%-- direction --%>
	              <c:when test="${columnStatus.count == 6}">
	                <td class="direction}">${column}</td>
	              </c:when>
	              <c:otherwise>
	                <td>${column}</td>
	              </c:otherwise>
	            </c:choose>
	          </c:forEach>
	        </tr>
	      </c:forEach>
      </tbody>
    </table>
  </div>

<script type="text/javascript">
	(function() {
	    <%-- no value... --%>
	    jQuery("#snp-to-gene-displayer.collection-table table td").each(function() {
	      if (jQuery(this).text() == "[no value]") jQuery(this).text("");
	    });

	    <%-- distance formatting --%>
	    jQuery("#snp-to-gene-displayer.collection-table table td.distance").each(function() {
	        var distance = parseInt(jQuery(this).text());
	        <%-- under 1kb --%>
	        if (distance < 1000) {
	          if (distance == 0) {
	            jQuery(this).text("genic");
	          } else {
	            jQuery(this).text(distance + "b");
	          }
	        } else {
	          jQuery(this).text(distance/1000 + "kb");
	        }
	    });
	})();
</script>

<style> div.geneInformation table td.distance { border-right:none; } </style>