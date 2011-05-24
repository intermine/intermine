<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>


<!-- friendlyMineLinkDisplayer.jsp -->

<c:set var="interMineObject" value="${object}"/>

<script type="text/javascript" charset="utf-8">

function getFriendlyMineLinks(mine, url, organisms, identifierList) {

    AjaxServices.getFriendlyMineListLinks(mine, organisms, identifierList, function(mineString) {
        // switch off loading img
        jQuery('#intermine_orthologue_links_' + mine).toggleClass('loading');
        if (mineString) {
            // parse to JSON (requires jQuery 1.4.1+)
            var jSONObject = jQuery.parseJSON(mineString);
            // TODO can be many identifiers, need to post
            // [{"identifiers":["ENSRNOG00000001410","ENSRNOG00000001575"],"isHomologue":false,"shortName":"R. norvegicus"}]
            generate(jSONObject, "#intermine_orthologue_links_" + mine, url);
        } else {
            jQuery("#intermine_orthologue_links_" + mine).html("No results found.");
        }
    });
}

  function generate(jSONObject, target, url) {
      // for each organism for which the mine has orthologues
      jQuery.each(jSONObject, function(key, entry) {
        if (entry['identifiers'] != undefined) {
            var homologue = '';
            if (entry['isHomologue'] == true) {
                homologue = "&orthologue=" + entry['shortName'];
            }
            var linky = "<li><a href='" + url + "/portal.do?externalids=" + entry['identifiers']  + "&class=Gene&origin=FlyMine" + homologue + "'>" + entry['shortName'] + "</a>";
            jQuery(target).html(linky);
        }
      });
  }

</script>

<h3>View orthologues in other Mines:</h3>

<tiles:importAttribute />

<c:forEach items="${mines}"  var="entry">
    <b>${entry.key}</b><div id="intermine_orthologue_links_${entry.key}" class="loading">&nbsp;</div><br/>
      <script type="text/javascript" charset="utf-8">
        getFriendlyMineLinks('${entry.key}', '${entry.value}', '${organisms}', '${identifierList}');
      </script>
</c:forEach>

<!-- /friendlyMineLinkDisplayer.jsp -->
