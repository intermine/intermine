<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>


<c:if test="${object.organism.taxonId == '7227'}">
  <p>
    <html:link href="http://www.flyexpress.net/search.php?type=gene&search=${object.organismDbId}" target="_new">
      <html:img src="model/images/flyexpress.png" title="Click here to visit the FlyExpress website" />${object.organismDbId}
    </html:link>
  </p>
</c:if>
