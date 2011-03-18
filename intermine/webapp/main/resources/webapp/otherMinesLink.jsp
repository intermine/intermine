<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<c:if test="${gogogo != null}">
  <script type="text/javascript" charset="utf-8">
  function getInterMineLinks(organismShortName, identifier, symbol) {
      AjaxServices.getInterMineLinks(organismShortName, identifier, symbol, function(mines) {
          // switch off loading img
          jQuery('#intermine_links').toggleClass('loading');
          jQuery('#intermine_links').html(mines);
          // parse to JSON (requires jQuery 1.4.1+)
          //var jSONObject = jQuery.parseJSON(mines);
      });

      //jQuery('#intermine_links').toggleClass('loading');
      //var object = [{"mineName":"modMine","gene":{"organismName":"D. melanogaster","identifier":"zen"},"orthologues":[{"organismName":"D. melanogaster","identifier":"eve"},{"organismName":"D. melanogaster","identifier":"CG30401"},{"organismName":"D. melanogaster","identifier":"FBgn0004054"},{"organismName":"D. melanogaster","identifier":"FBgn0000606"},{"organismName":"D. melanogaster","identifier":"zen2"},{"organismName":"D. melanogaster","identifier":"FBgn0050401"},{"organismName":"C. elegans","identifier":"WBGene00006873"},{"organismName":"C. elegans","identifier":"vab-7"}]}];
      //generate(object, "#intermine_links");
  }

  function generate(jSONObject, target) {
      jQuery(target).html("<ul></ul>");
      target += " ul";

      // for each mine in question...
      jQuery.each(jSONObject, function(key, entry) {
          // bag
          var bag = {}

          // gene
          var arr = new Array();
          arr.push(entry['gene']['identifier'])
          bag[entry['gene']['organismName']] = arr;
          alert(bag[entry['gene']['organismName']][0]);

          // for each orthologue
          jQuery.each(entry['orthologues'], function(orthKey, orthEntry) {
          });
      });
  }
  </script>

  <h3>Link to other InterMines</h3>
      <div id="intermine_links" class="loading">&nbsp;</div><br>
        <script type="text/javascript" charset="utf-8">
          getInterMineLinks('${object.organism.shortName}','${object.primaryIdentifier}','${object.symbol}');
        </script>
</c:if>