<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<html:xhtml/>

  <div class="custom-displayer">
    <h3 class="overlapping">SNPs to overlapping Genes within 10.0kb</h3>

    <table cellspacing="0" class="snpToGenes displayer">
      <tr>
        <th>Gene Primary Identifier</th>
        <th>Gene name</th>
        <th>Gene Symbol</th>
        <th colspan="2">Relative to Gene</th>
      </tr>
      <c:forEach var="row" items="${list}" varStatus="status">
        <tr class="${status.count mod 2 == 0 ? 'odd' : 'even'}">
          <c:forEach var="column" items="${row}" varStatus="columnStatus">
            <c:set var="tdStyle" value="${status.count mod 2 == 0 ? 'theme-3-border theme-6-background' : ''}" />
            <c:choose>
              <%-- primaryIdentifier & internalID --%>
              <c:when test="${columnStatus.count == 1}">
                <td class="${tdStyle}"><a title="Go to Gene page" href="report.do?id=${column}">
              </c:when>
              <%-- primaryIdentifier & internalID (cont...) --%>
              <c:when test="${columnStatus.count == 2}">
                ${column}</a></td>
              </c:when>

              <%-- distance --%>
              <c:when test="${columnStatus.count == 5}">
                <td class="distance ${tdStyle}">${column}</td>
              </c:when>
              <%-- direction --%>
              <c:when test="${columnStatus.count == 6}">
                <td class="direction ${tdStyle}">${column}</td>
              </c:when>
              <c:otherwise>
                <td class="${tdStyle}">${column}</td>
              </c:otherwise>
            </c:choose>
          </c:forEach>
        </tr>
      </c:forEach>
    </table>
  </div>

<script type="text/javascript">
    // no value...
    jQuery("div.custom-displayer table.displayer.snpToGenes td").each(function() {
      if (jQuery(this).text() == "[no value]") jQuery(this).text("");
    });

    // distance formatting
    jQuery("div.custom-displayer table.displayer.snpToGenes td.distance").each(function() {
        var distance = parseInt(jQuery(this).text());
        // under 1kb
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
</script>

<style> div.geneInformation table td.distance { border-right:none; } </style>