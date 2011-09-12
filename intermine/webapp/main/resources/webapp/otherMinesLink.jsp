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

  function getMineLinks(organismShortName, identifier, symbol) {
      AjaxServices.getFriendlyMineReportLinks(organismShortName, identifier, symbol, function(mines) {
          // switch off loading img
          jQuery('#intermine_links').toggleClass('loading');
          // parse to JSON (requires jQuery 1.4.1+)
          var jSONObject = jQuery.parseJSON(mines);
          generate(jSONObject, "#intermine_links");
      });
	  //var jSONObject = [{"mineName":"ZFINMine","organisms":[{"shortName":"D. rerio","orthologues":[{"displayIdentifier":"\"\"","primaryIdentifier":"ENSDARG00000044216"}]},{"genes":{"shortName":"H. sapiens","orthologues":{"displayIdentifier":"\"\"","primaryIdentifier":"ENSG00000140718"}},"shortName":"H. sapiens"}]}, {"mineName":"modMine"}, {"mineName":"YeastMine"}, {"mineName":"FlyMine","organisms":[{"genes":{"shortName":"H. sapiens","orthologues":{"displayIdentifier":"ENSG00000140718","primaryIdentifier":"FTO"}},"shortName":"H. sapiens"}]}, {"mineName":"RatMine"}];
  }

  function generate(jSONObject, target) {
	  jQuery('<ul/>', {
    	  'class': 'mines'
      })
      .appendTo(target);
	  target += ' ul.mines';
      
      // for each mine in question...
      jQuery.each(jSONObject, function(key, entry) {
          if (entry['organisms'] != undefined) {
              // details dict
              var minePortalDetails = minePortals[entry['mineName'].toLowerCase()];

              var organismAttribute = '';   // only used for orthologue links

              // mine
              if (minePortalDetails["bgcolor"] != null && minePortalDetails["frontcolor"] != null) { // we have colors! aaaw, pretty...
                jQuery('<li/>', {
                    'id': 'mine-' + key,
                    'class': 'mine'
                })
                .append(jQuery('<span/>', {
                    'style': 'background-color:' + minePortalDetails["bgcolor"] + ';color:' + minePortalDetails["frontcolor"],
                    'text': entry['mineName']
                }))
                .append(jQuery('<ul/>', {
                    'class': 'organisms'
                }))
                .appendTo(target);
              } else {
                jQuery('<li/>', {
                    'id': 'mine-' + key,
                    'class': 'mine',
                    'text': entry['mineName']
                })
                .append(jQuery('<ul/>', {
                    'class': 'organisms'
                }))
                .appendTo(target);
              }

              // traverse organisms
              jQuery.each(entry['organisms'], function(organismKey, organismEntry) {
                  // organism
                  jQuery('<li/>', {
                      'class': 'organism-' + organismKey,
                      'text': organismEntry['shortName']
                  })
                  .append(jQuery('<ul/>', {
                      'class': 'entries'
                  }))
                  .appendTo(target + " li#mine-" + key + " ul.organisms");

                  // gene item
                  if (organismEntry['genes'] != undefined) {
                	  var ortho = organismEntry['genes']['orthologues'];
                	  jQuery('<li/>', {
                          'text': (ortho['displayIdentifier'] == '""') ? ortho['primaryIdentifier'] : ortho['displayIdentifier']
                      })
                      .appendTo(target + " li#mine-" + key + " ul.organisms li.organism-" + organismKey + " ul.entries");
                  }
                  // orthologues list
                  if (organismEntry['orthologues'] != undefined) {
                      jQuery.each(organismEntry['orthologues'], function(orthoKey, orthoEntry) {
                            var identifier = orthoEntry['displayIdentifier'];
                            if (identifier == '""') {
                                identifier = orthoEntry['primaryIdentifier'];
                            }

                          jQuery('<li/>', {
                              'text': identifier
                          })
                          .appendTo(target + " li#mine-" + key + " ul.organisms li.organism-" + organismKey + " ul.entries");
                      });
                  }
                  // add separators & linkify
                  jQuery(target + " li#mine-" + key + " ul.organisms li.organism-" + organismKey + " ul.entries li").each(function(i) {
                      if (minePortalDetails["url"] != null) { // we have mine portal link, linkify
                        var linkText = jQuery(this).text();
                        jQuery(this).html("<a href='" + minePortalDetails["url"] + "/portal.do?externalids=" + linkText + "&class=Gene&origin=FlyMine'>" + linkText + "</a>");
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

  <h3 class="goog">Link to other InterMines</h3>
      <div id="intermine_links" class="loading">&nbsp;</div><br>
        <script type="text/javascript" charset="utf-8">
          getMineLinks('${object.organism.shortName}','${object.primaryIdentifier}','${object.symbol}');
        </script>
</c:if>