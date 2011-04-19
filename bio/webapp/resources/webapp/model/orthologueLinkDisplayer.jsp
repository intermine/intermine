<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1" prefix="str" %>

<script type="text/javascript" charset="utf-8">
function getFriendlyMineLinks(mine, organisms, identifierList) {
    AjaxServices.getFriendlyMineListLinks(mine, organisms, identifierList, function(mineString) {
        if (!mineString) {
            mineString = "No orthologues found.";
        }
        jQuery('#intermine_orthologue_links_' + mine).html(mineString);
        jQuery('#intermine_orthologue_links_' + mine + '_waiting').hide();
    });
}
</script>

<!-- friendlyMineLinkDisplayer.jsp -->

<h2>View orthologues in other InterMines:</h2>

<tiles:importAttribute />

<c:forEach items="${mines}" var="mine">
    <b>${mine}</b><div id="intermine_orthologue_links_${mine}_waiting"><img src="images/wait30.gif" title="Searching..."/></div>
    <div id="intermine_orthologue_links_${mine}"></div>
      <script type="text/javascript" charset="utf-8">
        getFriendlyMineLinks('${mine}', '${organisms}', '${identifierList}');
      </script>
</c:forEach>

<!-- /friendlyMineLinkDisplayer.jsp -->
