<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1" prefix="str" %>


<!-- minePathwaysDisplayer.jsp -->
<div id="mine-pathway-displayer" class="collection-table">

<script type="text/javascript" charset="utf-8">
function getFriendlyMinePathways(mine, orthologues) {

    AjaxServices.getFriendlyMinePathways(mine, orthologues, function(pathways) {
        var jSONObject = jQuery.parseJSON(pathways);
        if (jSONObject['results'].length > 0) {
        	generateFriendlyMinePathways(jSONObject, "#intermine_pathways_" + mine, mine);
        } else {
            jQuery("#intermine_pathways_" + mine).html("No pathways found.");
     	    jQuery('#mine-pathway-displayer table thead th').each(function(i) {
    		   if (jQuery(this).text() == mine) jQuery(this).removeClass('loading');
    	    });
        }
    });
}

function generateFriendlyMinePathways(jSONObject, target, mine) {
	var url = '';
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
		   if (jQuery(this).find('span').text() == mine) jQuery(this).removeClass('loading');
	   });
	}
}

</script>

<h3>Pathways</h3>
    <!-- one column for each mine -->
    <table>
      <thead>
      <tr>
            <!-- this mine -->
            <th><span><c:out value="${WEB_PROPERTIES['project.title']}" escapeXml="false"/></span></th>

            <!-- other mines -->
            <c:forEach items="${mines}" var="entry">
                 <th class="loading"><span>${entry.key.name}</span></th>
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
              <ul>
            <c:forEach items="${gene.pathways}" var="pathway">
                <li><html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${pathway.id}"><c:out value="${pathway.name}"/></html:link></li>
            </c:forEach>
            </ul>
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

<script type="text/javascript">
var minePortals = {};
<c:forEach var="portal" items="${minePortals}">
    var mineDetails = {};
    <c:forEach var="portalDetail" items="${portal.value}">
        mineDetails["<c:out value='${portalDetail.key}'/>"] = "<c:out value='${portalDetail.value}'/>";
    </c:forEach>
    minePortals["<c:out value='${fn:toLowerCase(portal.key)}'/>"] = mineDetails;
</c:forEach>

jQuery('#mine-pathway-displayer table thead th').each(function(i) {
	var settings = minePortals[jQuery(this).text().toLowerCase()];
	if (settings != undefined) {
		jQuery(this).find('span').css({'backgroundColor':settings['bgcolor'], 'color':settings['frontcolor']});
	}
});

</script>
</div>
<!-- /publicationCountsDisplayer.jsp -->
