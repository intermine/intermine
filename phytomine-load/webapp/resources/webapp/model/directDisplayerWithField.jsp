<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<tiles:importAttribute name="expr" ignore="false"/>
<im:eval evalExpression="interMineObject.${expr}" evalVariable="outVal"/>
<c:set var="seqHeader" value="Sequence" />
<c:choose>
  <c:when test="${empty outVal}">
    No sequence 
  </c:when>
  <c:otherwise>
    <c:choose>
      <c:when test="${!empty interMineObject.length}">
          <im:value>${interMineObject.length}</im:value>&nbsp;<html:link action="directSequenceExporter?object=${interMineObject.id}&header=${seqHeader}" target="_new"><html:img styleClass="fasta" src="model/images/fasta.gif" title="FASTA" />
        </html:link>
      </c:when>
      <c:otherwise>
        <im:value>${outVal}</im:value>
      </c:otherwise>
    </c:choose>
  </c:otherwise>
</c:choose>
