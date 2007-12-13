<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!-- redflyDisplayer.jsp -->
<fmt:setBundle basename="model"/>

<c:set var="sitePrefix" value="http://redfly.ccr.buffalo.edu/?content=/view_detail.php&crm_id="/>


<c:if test="${fn:startsWith(object.accession, 'REDfly')}">
  <div>
    <c:set var="href" value='${sitePrefix}${fn:substringAfter(object.accession, ":")}'/>
    <html:link href='${sitePrefix}${fn:substringAfter(object.accession, ":")}'
               title="${object.accession}"
               target="view_window">
      <c:out value="REDfly: ${object.identifier}"/>
      <img src="images/ext_link.png" alt="Link out" title="Link out"/>
    </html:link>
  </div>
</c:if>
<!-- /redflyDisplayer.jsp -->