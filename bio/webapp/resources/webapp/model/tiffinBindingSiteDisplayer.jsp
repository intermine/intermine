<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!-- tiffinBindingSiteDisplayer.jsp -->
<fmt:setBundle basename="model"/>

<c:set var="sitePrefix" value="http://servlet.sanger.ac.uk/tiffin/motif.jsp?acc="/>

<c:set var="isBindingSite" value="false"/>

<c:if test="${fn:startsWith(object.identifier, 'TIFDMEM') && !empty object.motif}">
  <div>
    <html:link href="${sitePrefix}${object.motif.identifier}.2"
               title="Sanger Tiffin pages: ${object.motif.identifier}"
               target="view_window">
      <fmt:message key="tiffin.bindingSite.description"/>:
      <html:img src="model/Sanger_logo_small.png"/>
    </html:link>
  </div>
</c:if>
<!-- /tiffinBindingSiteDisplayer.jsp -->
