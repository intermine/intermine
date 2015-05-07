<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!-- esynListDisplayer.jsp -->

<c:if test="${!empty reportObject.object.symbol && !empty reportObject.object.organism.taxonId && !empty reportObject.object.interactions}">

<div id="cwhead">
    <h3 class="goog">esyN Network Diagram</h3>
</div>

<iframe name="esyn" class="seamless" scrolling="no" id="iframe"
src="http://www.esyn.org/app.php?embedded=true&type=Graph&query=${identifiers}&organism=${taxon}&interactionType=any&includeInteractors=true&source=intermine"
width="500" height="500"></iframe>

    <p>These are physical (yellow lines) and genetic (green lines) interactions from BioGRID. See <a href="http://esyn.org/">esyn</a> for details.


</c:if>

<!-- /esynListDisplayer.jsp -->
