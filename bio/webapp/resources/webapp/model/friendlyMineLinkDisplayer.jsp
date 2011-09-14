<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>

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
    jQuery('<ul/>', {
        'class': 'organisms'
      })
      .appendTo(target);
    target += ' ul.organisms';

      // for each organism for which the mine has orthologues
      var i = 3;
      jQuery.each(jSONObject, function(key, entry) {
        if (entry['identifiers'] != undefined) {
            var homologue = '';
            if (entry['isHomologue'] == true) {
                homologue = "&orthologue=" + entry['shortName'];
            }
            jQuery('<li/>', {
                'id': 'organism-' + key,
                'style': (i <= 0) ? 'display:none;' : '',
                'html': jQuery('<a/>', {
                    'href': url + "/portal.do?externalids=" + entry['identifiers']  + "&class=Gene&origin=FlyMine" + homologue,
                    'text': entry['shortName']
                })
            }).appendTo(target);
            i--;
        }
      });
      if (i < 0) {
          jQuery('<li/>', {
            'class': 'toggler',
              'html': jQuery('<a/>', {
                  'text': 'Show all',
                  'href': '#',
                  'click': function(e) {
                    jQuery(this).parents(':eq(1)').find('li:hidden').show();
                    jQuery(this).remove();
                    e.preventDefault();
                  }
              })
          }).appendTo(target);
      }
  }

</script>

<h3 class="goog">View orthologues in other Mines:</h3>

<tiles:importAttribute />
<div id="friendlyMines">
  <c:forEach items="${mines}" var="entry">
    <div class="mine">
      <span style="background:${entry.value.bgcolor};color:${entry.value.frontcolor};">${entry.key}</span>
      <div id="intermine_orthologue_links_${entry.key}" class="loading"></div>
      <script type="text/javascript" charset="utf-8">
        getFriendlyMineLinks('${entry.key}', '${entry.value.url}', '${organisms}', '${identifierList}');
      </script>
    </div>
  </c:forEach>
</div>
<!-- /friendlyMineLinkDisplayer.jsp -->
