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
      //var jSONObject = [{"mineName":"ZFINMine"}, {"mineName":"modMine","organisms":[{"genes":{"shortName":"D. melanogaster","orthologues":{"displayIdentifier":"ap","primaryIdentifier":"FBgn0000099"}},"shortName":"D. melanogaster"},{"shortName":"D. virilis","orthologues":[{"displayIdentifier":"Dsim\\GD10352","primaryIdentifier":"FBgn0182124"},{"displayIdentifier":"\"\"","primaryIdentifier":"FBgn0171416"},{"displayIdentifier":"\"\"","primaryIdentifier":"FBgn0017741"},{"displayIdentifier":"\"\"","primaryIdentifier":"FBgn0041262"},{"displayIdentifier":"Dpse\\ap","primaryIdentifier":"FBgn0064417"},{"displayIdentifier":"\"\"","primaryIdentifier":"FBgn0154548"},{"displayIdentifier":"Dana\\GF13843","primaryIdentifier":"FBgn0090870"},{"displayIdentifier":"\"\"","primaryIdentifier":"FBgn0064607"},{"displayIdentifier":"\"\"","primaryIdentifier":"FBgn0236786"},{"displayIdentifier":"\"\"","primaryIdentifier":"FBgn0127893"},{"displayIdentifier":"\"\"","primaryIdentifier":"FBgn0142349"}]}]}, {"mineName":"metabolicMine"}, {"mineName":"YeastMine"}, {"mineName":"RatMine"}]
  }

  function generate(jSONObject, target) {
      jQuery(target).html("<ul class='mines'></ul>");
      target += " ul.mines";

      // for each mine in question...
      jQuery.each(jSONObject, function(key, entry) {
          if (entry['organisms'] != undefined) {
              // details dict
              var minePortalDetails = minePortals[entry['mineName'].toLowerCase()];

              var organismAttribute = '';   // only used for orthologue links

              // mine
              if (minePortalDetails["bgcolor"] != null && minePortalDetails["frontcolor"] != null) { // we have colors! aaaw, pretty...
                jQuery('<li/>', {
                    id: 'mine-' + key,
                    className: 'mine'
                })
                .append(jQuery('<span/>', {
                    style: 'background-color:' + minePortalDetails["bgcolor"] + ';color:' + minePortalDetails["frontcolor"],
                    text: entry['mineName']
                }))
                .append(jQuery('<ul/>', {
                    className: 'organisms'
                }))
                .appendTo(target);
              } else {
                jQuery('<li/>', {
                    id: 'mine-' + key,
                    className: 'mine',
                    text: entry['mineName']
                })
                .append(jQuery('<ul/>', {
                    className: 'organisms'
                }))
                .appendTo(target);
              }

              // traverse organisms
              jQuery.each(entry['organisms'], function(organismKey, organismEntry) {
                  // organism
                  jQuery('<li/>', {
                      className: 'organism-' + organismKey,
                      text: organismEntry['shortName']
                  })
                  .append(jQuery('<ul/>', {
                      className: 'entries'
                  }))
                  .appendTo(target + " li#mine-" + key + " ul.organisms");

                  // gene item
                  if (organismEntry['genes'] != undefined) {
                      jQuery('<li/>', {
                          text: organismEntry['genes']['orthologues']['displayIdentifier']
                      })
                      .appendTo(target + " li#mine-" + key + " ul.organisms li.organism-" + organismKey + " ul.entries");

                      //identifier = organismEntry['genes']['orthologues']['primaryIdentifier'];
                  }
                  // orthologues list
                  if (organismEntry['orthologues'] != undefined) {
                      jQuery.each(organismEntry['orthologues'], function(orthoKey, orthoEntry) {
                            var identifier = orthoEntry['displayIdentifier'];
                            if (identifier == '""') {
                                identifier = orthoEntry['primaryIdentifier'];
                            }

                          jQuery('<li/>', {
                              text: identifier
                          })
                          .appendTo(target + " li#mine-" + key + " ul.organisms li.organism-" + organismKey + " ul.entries");

                          //identifier = orthoEntry['primaryIdentifier'];
                      });
                  }
                  // add separators & linkify
                  jQuery(target + " li#mine-" + key + " ul.organisms li.organism-" + organismKey + " ul.entries li").each(function(i) {
                      if (minePortalDetails["url"] != null) { // we have mine portal link, linkify
                        // sometimes we do not have a symbol and have a string of bunny ears...
                        //if (jQuery(this).text() != '""') {
                          var linkText = jQuery(this).text();
                        //} else {
                        //  var linkText = identifier;
                        //}
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

  <h3>Link to other InterMines</h3>
      <div id="intermine_links" class="loading">&nbsp;</div><br>
        <script type="text/javascript" charset="utf-8">
          getMineLinks('${object.organism.shortName}','${object.primaryIdentifier}','${object.symbol}');
        </script>
</c:if>