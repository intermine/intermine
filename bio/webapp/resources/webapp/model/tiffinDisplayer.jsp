<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!-- tiffinDisplayer.jsp -->
<fmt:setBundle basename="model"/>

<c:set var="sitePrefix" value="http://servlet.sanger.ac.uk/tiffin/motif.jsp?acc="/>

<c:set var="isBindingSite" value="false"/>

<c:if test="${fn:startsWith(object.identifier, 'TIFDMEM')}">
  <fmt:message key="tiffin.description"/>:
  <div>
    <html:link href="${sitePrefix}${object.identifier}.2"
               title="Sanger Tiffin pages: ${object.identifier}"
               target="view_window">
      <html:img src="model/Sanger_logo_small.png"/>
    </html:link>
  </div>
</c:if>
<!-- /tiffinDisplayer.jsp -->
