<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1" prefix="str" %>

<script type="text/javascript" charset="utf-8">
function getInterMineLinks(organismShortName, identifier) {
    AjaxServices.getInterMineLinks(organismShortName, identifier, function(mines) {
        jQuery('#intermine_links').html(mines);
    });
}
</script>

<!-- interMineLinks.jsp -->
    <div id="intermine_links"/><br>
      <script type="text/javascript" charset="utf-8">
        getInterMineLinks('${object.organism.shortName}','${object.primaryIdentifier}' );
      </script>
<!-- /interMineLinks.jsp -->
