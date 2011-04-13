<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>

<c:if test="${minePortals != null}">
  <script type="text/javascript" charset="utf-8">

  // Java HashMap to JavaScript Dictionary
  var minePortals = {};
  <c:forEach var="portal" items="${minePortals}">
      var mineDetails = {};
      <c:forEach var="portalDetail" items="${portal.value}">
          mineDetails["<c:out value='${portalDetail.key}'/>"] = "<c:out value='${portalDetail.value}'/>";
      </c:forEach>
      minePortals["<c:out value='${fn:toLowerCase(portal.key)}'/>"] = mineDetails;
  </c:forEach>

  function getInterMineLinks(organismShortName, identifier, symbol) {
      AjaxServices.getInterMineLinks(organismShortName, identifier, symbol, function(mines) {
          // switch off loading img
          jQuery('#intermine_links').toggleClass('loading');
          // parse to JSON (requires jQuery 1.4.1+)
          var jSONObject = jQuery.parseJSON(mines);
          generate(jSONObject, "#intermine_links");
      });
      //var jSONObject = [{"mineName":"ZFINMine","organisms":[{"shortName":"D. rerio","orthologues":[{"isConverted":false,"identifier":"evx1"},{"isConverted":false,"identifier":"evx2"}]}]}, {"mineName":"modMine","organisms":[{"genes":{"shortName":"D. melanogaster","orthologues":{"isConverted":false,"identifier":"eve"}},"shortName":"D. melanogaster"},{"shortName":"C. elegans","orthologues":[{"isConverted":true,"identifier":"WBGene00006873"},{"isConverted":true,"identifier":"vab-7"}]}]}, {"mineName":"metabolicMine","organisms":[{"shortName":"H. sapiens","orthologues":[{"isConverted":false,"identifier":"EVX1"},{"isConverted":false,"identifier":"EVX2"}]}]}, {"mineName":"YeastMine"}, {"mineName":"RatMine","organisms":[{"shortName":"R. norvegicus","orthologues":[{"isConverted":false,"identifier":"Evx2"},{"isConverted":false,"identifier":"Evx1"}]}]}];
  }

  function generate(jSONObject, target) {
      jQuery(target).html("<ul class='mines'></ul>");
      target += " ul.mines";

      // for each mine in question...
      jQuery.each(jSONObject, function(key, entry) {
          if (entry['organisms'] != undefined) {
              // details dict
              var minePortalDetails = minePortals[entry['mineName'].toLowerCase()];
              var identifier;
              var organismAttribute = '';   // only used for orthologue links

              // mine
              if (minePortalDetails["bgcolor"] != null && minePortalDetails["frontcolor"] != null) { // we have colors! aaaw, pretty...
                jQuery(target).append("<li id='mine-" + key + "' class='mine'><span style='background-color:" + minePortalDetails["bgcolor"] + ";color:" + minePortalDetails["frontcolor"] + ";'>" + entry['mineName'] + "</span><ul class='organisms'></ul></li>");
              } else {
                jQuery(target).append("<li id='mine-" + key + "' class='mine'>" + entry['mineName'] + "<ul class='organisms'></ul></li>");
              }

              // traverse organisms
              jQuery.each(entry['organisms'], function(organismKey, organismEntry) {
                  // organism
                  jQuery(target + " li#mine-" + key + " ul.organisms").append("<li class='organism-" + organismKey + "'>" + organismEntry['shortName'] + "<ul class='entries'></ul></li>");
                  // gene item
                  if (organismEntry['genes'] != undefined) {
                      jQuery(target + " li#mine-" + key + " ul.organisms li.organism-" + organismKey + " ul.entries").append("<li>" + organismEntry['genes']['orthologues']['displayIdentifier'] + "</li>");
                      identifier = organismEntry['genes']['orthologues']['primaryIdentifier'];
                  }
                  // orthologues list
                  if (organismEntry['orthologues'] != undefined) {
                      jQuery.each(organismEntry['orthologues'], function(orthoKey, orthoEntry) {
                          jQuery(target + " li#mine-" + key + " ul.organisms li.organism-" + organismKey + " ul.entries").append("<li>" + orthoEntry['displayIdentifier'] + "</li>");
                          identifier = orthoEntry['primaryIdentifier'];
                      });
                  }
                  // add separators & linkify
                  jQuery(target + " li#mine-" + key + " ul.organisms li.organism-" + organismKey + " ul.entries li").each(function(i) {
                      if (minePortalDetails["url"] != null) { // we have mine portal link, linkify
                        jQuery(this).html("<a href='" + minePortalDetails["url"] + "/portal.do?externalids=" + identifier + "&class=Gene&origin=FlyMine'>" + jQuery(this).text() + "</a>");
                      }
                      if (i > 0) {
                        jQuery(this).html(", " + jQuery(this).html());
                      }
                  });
              });
          }
      });
  }
  </script>

  <h3>Link to other InterMines</h3>
      <div id="intermine_links" class="loading">&nbsp;</div><br>
        <script type="text/javascript" charset="utf-8">
          getInterMineLinks('${object.organism.shortName}','${object.primaryIdentifier}','${object.symbol}');
        </script>
</c:if>