<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<c:if test="${gogogo != null}">
  <script type="text/javascript" charset="utf-8">
  function getInterMineLinks(organismShortName, identifier, symbol) {
      AjaxServices.getInterMineLinks(organismShortName, identifier, symbol, function(mines) {
          // switch off loading img
          jQuery('#intermine_links').toggleClass('loading');
          // parse to JSON (requires jQuery 1.4.1+)
          var jSONObject = jQuery.parseJSON(mines);
          generate(jSONObject, "#intermine_links");
      });
  }

  function generate(jSONObject, target) {
      jQuery(target).html("<ul></ul>");
      target += " ul";

      // for each mine in question...
      jQuery.each(jSONObject, function(key, entry) {
          if (entry['organisms'] != undefined) {
              // mine
              jQuery(target).append("<li id='mine-" + key + "' class='" + entry['mineName'] + "'>" + entry['mineName'] + "<ul class='organisms'></ul></li>");
              jQuery.each(entry['organisms'], function(organismKey, organismEntry) {
                  // organism
                  jQuery(target + " li#mine-" + key + " ul.organisms").append("<li class='organism-" + organismKey + "'>" + organismEntry['shortName'] + "<ul class='entries'></ul></li>");
                  // gene item
                  if (organismEntry['genes'] != undefined) {
                      jQuery(target + " li#mine-" + key + " ul.organisms li.organism-" + organismKey + " ul.entries").append("<li>" + organismEntry['genes']['orthologues']['identifier'] + "</li>");
                  }
                  // orthologues list
                  if (organismEntry['orthologues'] != undefined) {
                      jQuery.each(organismEntry['orthologues'], function(orthoKey, orthoEntry) {
                          jQuery(target + " li#mine-" + key + " ul.organisms li.organism-" + organismKey + " ul.entries").append("<li>" + orthoEntry['identifier'] + "</li>");
                      });
                  }
                  // add separators & linkify
                  jQuery(target + " li#mine-" + key + " ul.organisms li.organism-" + organismKey + " ul.entries li").each(function(i) {
                      // http://www.intermine.org/rgd/portal.do?externalids=ENSRNOG00000007950&class=Gene&origin=FlyMine
                      jQuery(this).html("<a href='http://www.intermine.org/rgd/portal.do?externalids=" + jQuery(this).text() + "&class=Gene&origin=FlyMine'>" + jQuery(this).text() + "</a>");
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