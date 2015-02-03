<!-- alignmentPreview.jsp -->
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<tiles:importAttribute name="expr" ignore="false"/>
<im:eval evalExpression="interMineObject.${expr}" evalVariable="outVal"/>
<c:choose>
  <c:when test="${empty outVal}">
    Singleton cluster - no MSA
  </c:when>
  <c:otherwise>
    <html:link action="report?id=${interMineObject.msa.id}" target="_new"> View MSA </html:link>
  </c:otherwise>
</c:choose>
