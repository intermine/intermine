<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>

  <script type="text/javascript" charset="utf-8">

  function getFriendlyMineLinks(mine, url, organismShortName, identifier, symbol) {
      AjaxServices.getFriendlyMineReportLinks(mine, organismShortName, identifier, symbol, function(organisms) {
          jQuery('#intermine_orthologue_links_' + mine).toggleClass('loading');
          if (organisms) {
            var jSONObject = jQuery.parseJSON(organisms);
            generateMineLinks(jSONObject, url, organismShortName, "#intermine_orthologue_links_" + mine);
          } else {
            jQuery("#intermine_orthologue_links_" + mine).html("No results found.");
          }
      });
  }

  function generateMineLinks(jSONObject, url, organismShortName, target) {

    jQuery('<ul/>', {
        'class': 'organisms'
    })
    .appendTo(target);
    target += ' ul.organisms';


    jQuery.each(jSONObject, function(key, entry) {
        if (entry['genes'] != undefined) {
            jQuery.each(entry['genes'], function(geneKey, geneEntry) {
                var identifier = geneEntry['displayIdentifier'];
                if (identifier == '""') {
                    identifier = geneEntry['primaryIdentifier'];
                }
                jQuery('<li/>', {
                    'text': identifier
                })
                .appendTo(target);
            });
        }
    });

    // add separators & linkify
    jQuery(target + " li").each(function(i) {
        var homologue = '';
        if (organismShortName == jSONObject['shortName']) {
            homologue = '&orthologue=' + organismShortName;
        }
        jQuery(this).html(
            jQuery('<a/>', {
                'href': url + "/portal.do?externalids=" + jQuery(this).text() + homologue + "&class=Gene&origin=FlyMine",
                'text': jQuery(this).text(),
                'target': '_blank'
         }));
    });
 }
  </script>

<h3 class="goog">Link to other Mines</h3>
<div id="friendlyMines">
  <c:forEach items="${otherMines}" var="mine">
    <div class="mine">
      <span style="background:${mine.bgcolor};color:${mine.frontcolor};">${mine.name}</span>
      <div id="intermine_orthologue_links_${mine.name}" class="loading"></div>
      <script type="text/javascript" charset="utf-8">
        getFriendlyMineLinks('${mine.name}', '${mine.url}', '${object.organism.shortName}','${object.primaryIdentifier}','${object.symbol}');
      </script>
    </div>
  </c:forEach>
</div>
