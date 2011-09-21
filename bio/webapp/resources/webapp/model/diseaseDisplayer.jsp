<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1" prefix="str" %>


<!-- diseaseDisplayer.jsp -->

<script type="text/javascript" charset="utf-8">
function getRatDiseases(orthologues) {

    AjaxServices.getRatDiseases(orthologues, function(diseases) {
        var jSONObject = jQuery.parseJSON(diseases);
        // switch off loading img
        jQuery('#intermine_rat_disease').toggleClass('loading');
        if (jSONObject && jSONObject['results'].length > 0) {
            generateDiseases(jSONObject, "#intermine_rat_disease");
        } else {
            jQuery("#intermine_rat_disease").html("No diseases found.");
        }
    });
}

function generateDiseases(jSONObject, target) {
          var url = '';
          if (jSONObject['mineURL'] != undefined) {
            url = jSONObject['mineURL'];
          }

          if (jSONObject['results'] != undefined) {
             jQuery('<ul/>').appendTo(target);
             jQuery.each(jSONObject['results'], function(index, pathway) {
                  jQuery('<li/>', {
                    'html': jQuery('<a/>', {
                    'href': url + "/report.do?id=" + pathway['id'],
                    'text': pathway['name'],
                    'target': '_blank'
                })
                }).appendTo(target);
             });
          }
}

</script>

<div id="mine-rat-disease" class="collection-table">
<h3>Diseases (from RatMine)</h3>

    <!-- one column for each mine -->
    <table>
      <tbody>
      <tr>
        <td>
            <div id="intermine_rat_disease" class="loading"></div>
            <script type="text/javascript" charset="utf-8">
                getRatDiseases('${ratGenes}');
            </script>
          </td>
      </tr>
      </tbody>
    </table>

</div>
<!-- /diseaseDisplayer.jsp -->
