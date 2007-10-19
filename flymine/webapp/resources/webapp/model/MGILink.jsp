<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>


<c:if test="${object.organism.taxonId == '10090'}">
  <p><!-- http://www.informatics.jax.org/javawi2/servlet/WIFetch?page=searchTool&query=${object.organismDbId}&selectedQuery=Accession+IDs -->
    <html:link href="http://www.informatics.jax.org/javawi2/servlet/WIFetch?page=markerDetail&id=${object.organismDbId}" target="_new">
      <html:img src="model/images/mgi_logo.jpg" title="Click here to visit the MGI website" />${object.organismDbId}
    </html:link>
  </p>
</c:if>
