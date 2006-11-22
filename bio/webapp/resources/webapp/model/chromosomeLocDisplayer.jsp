<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- chromosomeLocDisplayer.jsp -->

<html:xhtml/>
<c:choose>
  <c:when test="${!empty interMineObject.chromosome}">
    <b>
      ${interMineObject.chromosome.identifier}<c:if test="${!empty interMineObject.chromosomeLocation && !empty interMineObject.chromosomeLocation.start}">
        : ${interMineObject.chromosomeLocation.start}-${interMineObject.chromosomeLocation.end}
      </c:if>
    </b>
  </c:when>
  <c:otherwise>
    [unknown]
  </c:otherwise>
</c:choose>

<!-- /chromosomeLocDisplayer.jsp -->
