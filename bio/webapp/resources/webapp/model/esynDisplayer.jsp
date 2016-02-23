<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!-- esynDisplayer.jsp -->

<c:if test="${!empty reportObject.object.primaryIdentifier && !empty reportObject.object.organism.taxonId && !empty reportObject.object.interactions}">

  <div id="cwhead">
      <h3 class="goog">esyN Network Diagram</h3>
  </div>
  <c:choose>
    <c:when test="${reportObject.object.interactions.size() < 500}">
      <c:set var="primaryIdentifier" value="${reportObject.object.primaryIdentifier}"/>
      <c:set var="taxon" value="${reportObject.object.organism.taxonId}"/>

      <iframe name="esyn" class="seamless" scrolling="no" id="iframe"
        src="http://www.esyn.org/app.php?embedded=true&type=Graph&query=${primaryIdentifier}&organism=${taxon}&interactionType=any&includeInteractors=true&source=intermine"
        width="500" height="500"></iframe>

      <p>Physical (Orange) and Genetic (Green) interactions. Visit <a href="http://esyn.org/">esyN</a> for more details or click the Edit in esyN button if you wish to extend or modify the network.</p>
    </c:when>
    <c:otherwise>
      <%-- Let's display a sane error message if there are too many results.
    This was implemented due to https://github.com/intermine/intermine/issues/1284, where > 2000 interactions caused the browser to halt. --%>
    <p>${reportObject.object.interactions.size()} interactions found. The esyN viewer is not displayed with this high number of interactions in order to enhance your browser's performance.</p>
    </c:otherwise>
  </c:choose>
</c:if>

<!-- /esynDisplayer.jsp -->
