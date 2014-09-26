<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>

<!-- friendlyMineLinkDisplayer.jsp -->

<c:set var="interMineObject" value="${object}"/>

<script type="text/javascript" charset="utf-8">

function getFriendlyMineLinks(mine, url, organisms, identifiers) {
    AjaxServices.getFriendlyMineLinks(mine, organisms, identifiers, function(mineString) {
        // switch off loading img
        jQuery('#intermine_orthologue_links_' + mine).toggleClass('loading');
        if (mineString) {
            var jSONObject = jQuery.parseJSON(mineString);
            // [{"genes":["ENSRNOG00000001410","ENSRNOG00000001575"],"isHomologue":false,"shortName":"R. norvegicus"}]
            generate(jSONObject, "#intermine_orthologue_links_" + mine, url, organisms);
        } else {
            jQuery("#intermine_orthologue_links_" + mine).html("No results found.");
        }
    });
}

  function generate(jSONObject, target, url, organism) {
    jQuery('<ul/>', {
        'class': 'organisms'
      })
      .appendTo(target);
    target += ' ul.organisms';

      // for each organism for which the mine has orthologues
      var i = 3;
      jQuery.each(jSONObject, function(key, entry) {
        if (entry['identifiers'] != undefined) {
            var homologue;
            if (entry['shortName'] != organism) {
              homologue = jQuery('<input/>', {
                'name': 'orthologue',
                'value': entry['shortName']
              })
            }
            jQuery('<li/>', {
                'id': 'organism-' + key,
                'style': (i <= 0) ? 'display:none;' : '',
                'html': jQuery('<a/>', {
                  'href': '#',
                    'text': entry['shortName'],
                    'target': '_blank',
                    click: function(e) {
                      var form = jQuery('<form/>', {
                        'target': '_blank',
                        'method': 'post',
                        'action': url + '/portal.do',
                        'style': 'display:none;'
                      })
                      .append(jQuery('<input/>', {
                        'name': 'externalids',
                        'value': entry['identifiers']
                      }))
                      .append(jQuery('<input/>', {
                        'name': 'class',
                        'value': 'Gene'
                      }))
                      .append(jQuery('<input/>', {
                        'name': 'origin',
                        'value': 'FlyMine'
                      }))
                      .append(homologue)
                      .appendTo('#friendlyMines');

                      //form.find('input').each(function() {
                      //  im.log(jQuery(this).attr('name') + ": " + jQuery(this).val());
                      //});

                      form.submit()
                      form.remove();

                      e.preventDefault();
                    }
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

<h3 class="goog">View homologues in other Mines:</h3>

<tiles:importAttribute />
<div id="friendlyMines">
  <c:forEach items="${mines}" var="mine">
    <div class="mine">
      <span style="background:${mine.bgcolor};color:${mine.frontcolor};">${mine.name}</span>
      <div id="intermine_orthologue_links_${mine.name}" class="loading"></div>
      <script type="text/javascript" charset="utf-8">
        getFriendlyMineLinks('${mine.name}', '${mine.url}', '${organisms}', '${identifiers}');
      </script>
    </div>
  </c:forEach>
</div>
<!-- /friendlyMineLinkDisplayer.jsp -->
