<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1" prefix="str" %>


<!-- minePathwaysDisplayer.jsp -->
<script type="text/javascript" charset="utf-8">
function getFriendlyMinePathways(mine, orthologues) {

    AjaxServices.getFriendlyMinePathways(mine, orthologues, function(pathways) {
        im.log(pathways);
        var jSONObject = jQuery.parseJSON(pathways);
        if (jSONObject['results'].length > 0) {
            generate(jSONObject, "#intermine_pathways_" + mine, mine);
        } else {
            jQuery("#intermine_pathways_" + mine).html("No pathways found.");
        }
    });
}

function generate(jSONObject, target, mine) {
	var url = '';
	im.log(jSONObject);
	if (jSONObject['mineURL'] != undefined) {
	  url = jSONObject['mineURL'];
	}
	
	if (jSONObject['results'] != undefined) {
	   jQuery('<ul/>').appendTo(target);
	   jQuery.each(jSONObject['results'], function(index, pathway) {
	        jQuery('<li/>', {
	          'html': jQuery('<a/>', {
	          'href': url + "/report.do?id=" + pathway['id'],
	          'text': pathway['name'],
	          'target': '_blank'
	      })
	      }).appendTo(target + ' ul');
	   });
	   
	   
	   jQuery('#mine-pathway-displayer table thead th').each(function(i) {
		   if (jQuery(this).text() == mine) jQuery(this).removeClass('loading');
	   });
	} else {
	   jQuery(target).closest('td').remove();
	}
}

</script>

<div id="mine-pathway-displayer" class="collection-table">
<h3>Pathways</h3>


    <!-- one column for each mine -->
    <table>
      <thead>
      <tr>
            <!-- this mine -->
            <th><c:out value="${WEB_PROPERTIES['project.title']}" escapeXml="false"/></th>

            <!-- other mines -->
            <c:forEach items="${mines}" var="entry">
                 <th class="loading">${entry.key.name}</th>
            </c:forEach>
      </tr>
	  </thead>
	  <tbody>
      <tr>
          <!-- this mine -->
        <td>

            <c:choose>
              <c:when test="${empty gene.pathways}">
                No pathways found
              </c:when>
              <c:otherwise>
            <c:forEach items="${gene.pathways}" var="pathway">
                <html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${pathway.id}"><c:out value="${pathway.name}"/></html:link><br/>
            </c:forEach>
            </c:otherwise>
            </c:choose>
        </td>

        <!-- other mines -->
        <c:forEach items="${mines}" var="entry">
            <td>
            <div id="intermine_pathways_${entry.key.name}"></div>
            <script type="text/javascript" charset="utf-8">
                getFriendlyMinePathways('${entry.key.name}', '${entry.value}');
            </script>
            </td>
        </c:forEach>
      </tr>
      </tbody>
    </table>

</div>
<!-- /publicationCountsDisplayer.jsp -->
