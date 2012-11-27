<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<im:instanceof instanceofObject="${interMineObject}" instanceofClass="org.intermine.model.bio.SequenceFeature" instanceofVariable="hasChromosomeLocation"/>
<im:instanceof instanceofObject="${interMineObject}" instanceofClass="org.intermine.model.bio.Location" instanceofVariable="isChromosomeLocation"/>
<c:choose>
  <c:when test="${hasChromosomeLocation == 'true' && !empty interMineObject.chromosomeLocation && !empty interMineObject.chromosomeLocation.locatedOn}">
    ${interMineObject.chromosomeLocation.locatedOn.primaryIdentifier}<c:if test="${!empty interMineObject.chromosomeLocation && !empty interMineObject.chromosomeLocation.start}">: ${interMineObject.chromosomeLocation.start}-${interMineObject.chromosomeLocation.end}
    </c:if>
  </c:when>
  <c:when test="${isChromosomeLocation == 'true' && !empty interMineObject.locatedOn}">
    ${interMineObject.locatedOn.primaryIdentifier}<c:if test="${!empty interMineObject.start}">: ${interMineObject.start}-${interMineObject.end}
    </c:if>
  </c:when>
  <c:otherwise>
    [unknown]
  </c:otherwise>
</c:choose>
