<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- chromosomeLocDisplayer.jsp -->

<html:xhtml/>
<c:choose>
  <c:when test="${!empty interMineObject.chromosome}">
    ${interMineObject.chromosome.primaryIdentifier}<c:if test="${!empty interMineObject.chromosomeLocation && !empty interMineObject.chromosomeLocation.start}">: ${interMineObject.chromosomeLocation.start}-${interMineObject.chromosomeLocation.end}
    </c:if>
  </c:when>
  <c:otherwise>
    [unknown]
  </c:otherwise>
</c:choose>

<!-- /chromosomeLocDisplayer.jsp -->
