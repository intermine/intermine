<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1" prefix="str" %>


<!-- diseaseDisplayer.jsp -->

<div id="mine-rat-disease" class="collection-table">
<h3 class="loading">Diseases (from RatMine)</h3>

<table>
  <tbody>
  <tr>
    <td>
        <div id="intermine_rat_disease"></div>
      </td>
  </tr>
  </tbody>
</table>

<c:choose>
  <c:when test="${ratGenes != null && !empty(ratGenes)}">

<style type="text/css">
    .disease-item {
        float: left;
        display: inline-block;
        min-width: 30em;
        padding-bottom: 0.5em;
    }
    .less { display: none; }
</style>

<script type="text/javascript" charset="utf-8">
function generateDiseases(jSONObject, target) {
   var url, len, $ul;
   var maxRows = 12;
   var $morer = jQuery('<div class="toggle" style="margin-top: 5px"><a class="more">Show <span class="more-count"></span> more diseases</a></div>');
   if (jSONObject['mineURL'] != undefined) {
     url = jSONObject['mineURL'];
   }

   if (jSONObject.results != undefined) {
      $ul = jQuery('<ul/>').appendTo(target);
      len = jSONObject.results.length;
      jQuery.each(jSONObject.results, function(index, pathway) {
           jQuery('<li/>', {
             'html': jQuery('<a/>', {
             'href': url + "/report.do?id=" + pathway['id'],
             'text': pathway['name'],
               'target': '_blank'
             })
         }).appendTo($ul).addClass("disease-item").toggleClass("less", index >= maxRows);
      });
      jQuery(target).append('<div style="clear:both"></div>');
      if (len > maxRows) {
          $morer.appendTo(target).click(function() {
            jQuery(target).find('.less').show();
            jQuery(this).remove();
          }).find('.more-count').text(len - maxRows);
      }
   }
}

(function() {
    AjaxServices.getRatDiseases('${ratGenes}', function(response) {
        jQuery("#mine-rat-disease h3").removeClass('loading');
        if (response) {
            var jSONObject = jQuery.parseJSON(response);

            if (jSONObject["status"] != "offline") {
                if (jSONObject && jSONObject['results'].length > 0) {
                    generateDiseases(jSONObject, "#intermine_rat_disease");
                 } else {
                   jQuery("#intermine_rat_disease").html("<p>No diseases found.</p>");
                 }
            } else {
              jQuery("#intermine_rat_disease").html("<p>RatMine offline.</p>");
            }
       }
     });
})();

</script>

</c:when>
<c:otherwise>
  <!-- no rat homologues for this gene -->
  <script type="text/javascript">
    jQuery("#mine-rat-disease h3").removeClass('loading');
    jQuery("#intermine_rat_disease").html("<p>No diseases found.</p>");
  </script>
</c:otherwise>
</c:choose>

</div>
<!-- /diseaseDisplayer.jsp -->
