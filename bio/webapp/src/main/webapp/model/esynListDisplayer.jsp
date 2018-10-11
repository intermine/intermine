<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!-- esynListDisplayer.jsp -->

<c:if test="${fn:length(identifiers) < 8190}">

<div id="cwhead">
    <h3 class="goog">esyN Network Diagram</h3>
</div>

<iframe name="esyn" class="seamless" scrolling="no" id="iframe"
src="http://www.esyn.org/app.php?embedded=true&type=Graph&query=${identifiers}&organism=${taxon}&interactionType=any&includeInteractors=false&source=intermine"
width="500" height="500"></iframe>

    <p>Physical (Orange) and Genetic (Green) interactions between the genes in the list. Visit <a href="http://esyn.org/">esyN</a> for more details or click the Edit in esyN button if you wish to extend or modify the network.</p>

</c:if>

<!-- /esynListDisplayer.jsp -->
