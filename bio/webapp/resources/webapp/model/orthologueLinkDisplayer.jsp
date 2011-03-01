<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1" prefix="str" %>

<script type="text/javascript" charset="utf-8">
function getInterMineOrthologueLinks(mine, organisms) {
    AjaxServices.getInterMineOrthologueLinks(mine, organisms, function(mines) {
        jQuery('#intermine_orthologue_links').html(mines);
        jQuery('#intermine_orthologue_links_' + mine + '_waiting').hide();
    });
}
</script>

<!-- orthologueLinkDisplayer.jsp -->

<h2>View orthologues in other InterMines:</h2>

<tiles:importAttribute />

<form name="intermine_orthologue_links_form" method="post">
    <input type="hidden" id="externalids" name="externalids" value="${identifierList}"/>
    <input type="hidden" id="class" name="class" value="${bag.type}"/>
</form>

<c:forEach items="${mines}" var="mine">
    <div id="intermine_orthologue_links_${mine}_waiting"><img src="images/wait30.gif" title="Searching..."/></div>
    <div id="intermine_orthologue_links_${mine}"/><br>
      <script type="text/javascript" charset="utf-8">
        getInterMineOrthologueLinks('${mine}', '${organisms}');
      </script>
</c:forEach>
<!-- /orthologueLinkDisplayer.jsp -->
