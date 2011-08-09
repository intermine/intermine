<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>

<!-- friendlyMineLinkDisplayer.jsp -->

<style>
#friendlyMines b { display:block; clear:both; }
#friendlyMines div.feature ul li:nth-child(2n) { border-left:1px solid #AFAFAF; }
</style>

<c:set var="interMineObject" value="${object}"/>

<script type="text/javascript" charset="utf-8">
// Java HashMap to JavaScript Dictionary
var minePortals = {};
<c:forEach var="portal" items="${mines}">
    var mineDetails = {};
    <c:forEach var="portalDetail" items="${portal.value}">
        mineDetails["<c:out value='${portalDetail.key}'/>"] = "<c:out value='${portalDetail.value}'/>";
    </c:forEach>
    minePortals["<c:out value='${fn:toLowerCase(portal.key)}'/>"] = mineDetails;
</c:forEach>

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
      jQuery(target).html("<ul class='organisms'></ul>");
      target += " ul.organisms";
      // for each organism for which the mine has orthologues
      jQuery.each(jSONObject, function(key, entry) {
        if (entry['identifiers'] != undefined) {
            var homologue = '';
            if (entry['isHomologue'] == true) {
                homologue = "&orthologue=" + entry['shortName'];
            }
            jQuery('<li/>', {
                id: 'organism-' + key,
                html: jQuery('<a/>', {
                    href: url + "/portal.do?externalids=" + entry['identifiers']  + "&class=Gene&origin=FlyMine" + homologue,
                    text: entry['shortName']
                })
            }).appendTo(target);
        }
      });
  }

</script>

<h3 class="goog">View orthologues in other Mines:</h3>

<tiles:importAttribute />
<div id="friendlyMines">
  <c:forEach items="${mines}" var="entry">
    <b>${entry.key}</b>
    <div id="intermine_orthologue_links_${entry.key}" class="loading">&nbsp;</div>
    <script type="text/javascript" charset="utf-8">
      getFriendlyMineLinks('${entry.key}', '${entry.value.url}', '${organisms}', '${identifierList}');
    </script>
  </c:forEach>
</div>
<!-- /friendlyMineLinkDisplayer.jsp -->
