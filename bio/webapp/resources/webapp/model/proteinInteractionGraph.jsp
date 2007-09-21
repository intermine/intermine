<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<c:choose>
  <c:when test="${empty object.proteinInteractions}">
    <p>No protein interactions found in FlyMine</p>
  </c:when>
  <c:otherwise>
    <img style="border: 1px solid #ccc" title="Protein Interactions" 
         src="<html:rewrite action="/proteinInteractionRenderer?object=${object.id}"/>"/>
  </c:otherwise>
</c:choose>
