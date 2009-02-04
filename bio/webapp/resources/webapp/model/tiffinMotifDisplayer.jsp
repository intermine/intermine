<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!-- tiffinDisplayer.jsp -->


<c:set var="sitePrefix" value="http://servlet.sanger.ac.uk/tiffin/motif.jsp?acc="/>

<c:set var="isBindingSite" value="false"/>

<c:if test="${fn:startsWith(object.primaryIdentifier, 'TIFDMEM')}">
  <div>
    <html:link href="${sitePrefix}${object.primaryIdentifier}.2"
               title="Sanger Tiffin pages: ${object.primaryIdentifier}"
               target="view_window">
      <fmt:message key="tiffin.description"/>:
      <html:img src="model/images/Sanger_logo_small.png"/>
    </html:link>
  </div>
</c:if>
<!-- /tiffinDisplayer.jsp -->
