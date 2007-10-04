<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>


<c:if test="${object.organism.taxonId == '180454'}">
  <p>
    <html:link href="http://agambiae.vectorbase.org/Genome/GeneView/?gene=${object.organismDbId}" target="_new">
      <html:img src="model/images/VectorBase_logo_small.png" title="Click here to visit the VectorBase website" />${object.organismDbId}
    </html:link>
  </p>
</c:if>
