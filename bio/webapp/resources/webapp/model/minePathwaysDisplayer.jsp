<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1" prefix="str" %>


<!-- minePathwaysDisplayer.jsp -->
<div id="mine-pathway-displayer" class="collection-table column-border">

<script type="text/javascript" charset="utf-8">
function getFriendlyMinePathways(mine, orthologues) {

    AjaxServices.getFriendlyMinePathways(mine, orthologues, function(pathways) {
      var jSONObject = jQuery.parseJSON(pathways),
        position;

      jQuery('#mine-pathway-displayer table thead th').each(function(i) {
            if (jQuery(this).text() == mine) position = i;
           });

        if (jSONObject && jSONObject['results'].length > 0) {
          generateFriendlyMinePathways(jSONObject, "#intermine_pathways_" + mine, mine);
        } else {
            jQuery("#intermine_pathways_" + mine).text('None');
        }
       jQuery('#mine-pathway-displayer table thead th:eq(' + position + ')').removeClass('loading');
    });
}

function generateFriendlyMinePathways(jSONObject, target, mine) {
  var url = '';
  if (jSONObject['mineURL'] != undefined) {
    url = jSONObject['mineURL'];
  }

  if (jSONObject['results'] != undefined) {
     jQuery('<ul/>').appendTo(target);
     var i;
     jQuery.each(jSONObject['results'], function(index, pathway) {
          jQuery('<li/>', {
            'html': jQuery('<a/>', {
            'href': url + "/report.do?id=" + pathway['id'],
            'text': pathway['name'],
            'target': '_blank',
            'class': 'external',
            'style': (index > 9) ? 'display:none;' : ''
        })
        }).appendTo(target + ' ul');
        i = index;
     });
     if (i > 10) {
       jQuery('<div/>', {
         'class': 'toggle',
         'style': 'margin-top:5px'
       })
       .append(jQuery('<a/>', {
         'class': 'more',
         'text': 'Show ' + (i - 9) + ' more pathway' + (((i - 9) > 1) ? 's' : ''),
         'click': function() {
           jQuery(target).find(':hidden').show();
           jQuery(this).remove();
         }
       }))
       .appendTo(target);
     }
  }
}

</script>

<div class="header">
<h3>Pathways from Other Mines</h3>

<c:choose>
  <c:when test="${gene != null && !empty(gene) && ((minesForPathways != null && !empty(minesForPathways)) || !empty(gene.pathways))}">

      <p>
        <img class="tinyQuestionMark" src="images/icons/information-small-blue.png" alt="?">
        Pathway data from other Mines for homologues of this gene.
      </p>
      </div>

    <!-- one column for each mine -->
    <table>
      <thead>
      <tr>
            <!-- this mine -->
            <th><span><c:out value="${WEB_PROPERTIES['project.title']}" escapeXml="false"/></span></th>

            <!-- other mines -->
            <c:forEach items="${minesForPathways}" var="entry">
                <c:set var="mine" value="${entry.key}" />
                <c:set var="homologues" value="${entry.value}" />
                <th class="loading"><div class="mine"><span style="background:${mine.bgcolor};color:${mine.frontcolor};">${mine.name}</span></div></th>
            </c:forEach>
      </tr>
    </thead>
    <tbody>
      <tr>
          <!-- this mine -->
        <td>

            <c:choose>
              <c:when test="${empty gene.pathways}">
                None
              </c:when>
              <c:otherwise>
              <div id="intermine_pathways_thisMine">
              <ul>
              <c:forEach items="${gene.pathways}" var="pathway" varStatus="status">
                  <li>
                    <c:choose>
                      <c:when test="${status.index > 9}">
                        <c:set var="style" value="display:none;" />
                      </c:when>
                      <c:otherwise>
                        <c:set var="style" value="" />
                      </c:otherwise>
                    </c:choose>
                    <html:link style="${style}" href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${pathway.id}">
                      <c:out value="${pathway.name}"/>
                    </html:link>
                  </li>
              </c:forEach>
            </ul>
            </div>

            <c:if test="${fn:length(style) > 0}">
              <script type="text/javascript">
                 var target = '#intermine_pathways_thisMine ul',
                      i = jQuery(target).find('li').length - 10;
              jQuery('<div/>', {
               'class': 'toggle',
               'style': 'margin-top:5px'
             })
             .append(jQuery('<a/>', {
               'class': 'more',
               'text': 'Show ' + i + ' more pathway' + ((i > 1) ? 's' : ''),
               'click': function() {
                 jQuery(target).find(':hidden').show();
                 jQuery(this).remove();
               }
             }))
             .appendTo(target);
              </script>
            </c:if>

            </c:otherwise>
            </c:choose>
        </td>

        <!-- other mines -->
        <c:forEach items="${minesForPathways}" var="entry">
            <td>
            <c:set var="mine" value="${entry.key}" />
            <div id="intermine_pathways_${mine.name}"></div>
            <script type="text/javascript" charset="utf-8">
                getFriendlyMinePathways('${mine.name}', '${entry.value}');
            </script>
            </td>
        </c:forEach>
      </tr>
      </tbody>
    </table>

  </c:when>
  <c:otherwise>
  </div>
    <p>No pathways found.</p>
  </c:otherwise>

</c:choose>
</div>
<!-- /publicationCountsDisplayer.jsp -->
