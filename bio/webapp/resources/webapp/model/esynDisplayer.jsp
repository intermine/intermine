<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!-- esynDisplayer.jsp -->

<c:if test="${!empty reportObject.object.symbol && !empty reportObject.object.organism.taxonId}">

<div id="cwhead">
    <h3 class="goog">esyN Network Diagram</h3>
</div>
    
    <c:set var="symbol" value="${reportObject.object.symbol}"/>
    <c:set var="taxon" value="${reportObject.object.organism.taxonId}"/>


<iframe name="esyn" class="seamless" scrolling="no" id="iframe"
src="http://www.esyn.org/app.php?embedded=true&type=Graph&query=${symbol}&organism=${taxon}&interactionType=physical&includeInteractors=true&source=biogrid"
width="500" height="500"></iframe>

    <p>These are interactions from BioGRID. See <a href="http://esyn.org/">esyn</a> for details.


</c:if>

<!-- /esynDisplayer.jsp -->
