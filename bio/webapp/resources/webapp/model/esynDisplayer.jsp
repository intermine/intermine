<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!-- esynDisplayer.jsp -->

<c:if test="${!empty reportObject.object.primaryIdentifier && !empty reportObject.object.organism.taxonId && !empty reportObject.object.interactions}">

<div id="cwhead">
    <h3 class="goog">esyN Network Diagram</h3>
</div>

    <c:set var="primaryIdentifier" value="${reportObject.object.primaryIdentifier}"/>
    <c:set var="taxon" value="${reportObject.object.organism.taxonId}"/>

<iframe name="esyn" class="seamless" scrolling="no" id="iframe"
src="http://www.esyn.org/app.php?embedded=true&type=Graph&query=${primaryIdentifier}&organism=${taxon}&interactionType=any&includeInteractors=true&source=intermine"
width="500" height="500"></iframe>

    <p>Physical (Orange) and Genetic (Green) interactions. Visit <a href="http://esyn.org/">esyN</a> for more details or click the Edit in esyN button if you wish to extend or modify the network.</p>


</c:if>

<!-- /esynDisplayer.jsp -->
