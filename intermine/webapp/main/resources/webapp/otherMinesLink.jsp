<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>

<script type="text/javascript" charset="utf-8">

function getFriendlyMineLinks(mine, url, organismShortName, identifier) {
    AjaxServices.getFriendlyMineLinks(mine, organismShortName, identifier, function(response) {
      var jSONObject = jQuery.parseJSON(response)
      jQuery('#intermine_orthologue_links_' + mine).toggleClass('loading');
        if (jSONObject && jSONObject.length > 0) {
          generateMineLinks(jSONObject, url, organismShortName, "#intermine_orthologue_links_" + mine);
        } else {
          jQuery("#intermine_orthologue_links_" + mine).html("<p>No results found.</p>");
        }
    });
}

function generateMineLinks(jSONObject, url, organismShortName, target) {

  jQuery('<ul/>', {
      'class': 'organisms'
  })
  .appendTo(target);
  target += ' ul.organisms';

  <%-- traverse organisms --%>
  jQuery.each(jSONObject, function(k, organism) {
    var shortName = organism['shortName'];
      if (organism['genes'] != undefined) {
        <%-- create the organism list item --%>
        jQuery('<li/>', {
          'class': 'organism-' + k,
          'text': shortName
        })
        .append(function() {
          <%-- create a list of organism - genes found --%>
          return jQuery('<ul/>', {
            'class': 'entries'
          })
          .append(function() {
            var self = this;
            jQuery.each(organism['genes'], function(geneKey, gene) {
              jQuery('<li/>', {
                'class': 'gene-' + geneKey,
              })
              .append(jQuery('<a/>', {
                'text': (gene['displayIdentifier'] != '""') ? gene['displayIdentifier'] : gene['primaryIdentifier'],
                 'target': '_blank'
              })
              .attr('href', function() {
                var homologue = (organismShortName == shortName) ? '&orthologue=' + organismShortName : '';
                return (url + "/portal.do?externalids=" + gene['primaryIdentifier'] + homologue + "&class=Gene&origin=FlyMine").replace(/ /g, '+');
              })
              )
              .appendTo(self);
            });
          });
        })
        .appendTo(target);
      }
  });
}
</script>
<c:if test="${!empty otherMines}">
<h3 class="goog">Link to other Mines</h3>
<div id="friendlyMines">
  <c:forEach items="${otherMines}" var="mine">
    <div class="mine">
      <span style="background:${mine.bgcolor};color:${mine.frontcolor};">${mine.name}</span>
      <div id="intermine_orthologue_links_${mine.name}" class="loading"></div>
      <script type="text/javascript" charset="utf-8">
        getFriendlyMineLinks('${mine.name}', '${mine.url}', '${object.organism.shortName}','${object.primaryIdentifier}');
      </script>
    </div>
  </c:forEach>
</div>
</c:if>