<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- sequenceShortDisplayerWithField.jsp -->
<tiles:importAttribute name="expr" ignore="false"/>
<html:xhtml/>
<im:eval evalExpression="object.object.${expr}" evalVariable="outVal"/>
<c:choose>
  <c:when test="${empty outVal}">
    &nbsp;
  </c:when>
  <c:otherwise>
    <b><im:value>${outVal}</im:value></b>
    <html:link action="sequenceExporter?object=${object.id}">
      <html:img styleClass="fasta" src="model/fasta.gif"/>
    </html:link>
  </c:otherwise>
</c:choose>
<!-- /sequenceShortDisplayerWithField.jsp -->
