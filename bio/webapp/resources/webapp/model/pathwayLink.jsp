<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- pathway.jsp -->
<html:xhtml/>
  <div style="margin-left: 20px">
    <c:set var="sourceTitle" value="KEGG"/>
    <c:set var="linkProperty" value="${sourceTitle}.url.prefix"/>
    <html:img src="model/images/${sourceTitle}_logo_small.gif" title="${sourceTitle}" />
    <html:link href="${WEB_PROPERTIES[linkProperty]}map${object.identifier}"
               title="${sourceTitle}: ${object.identifier}"
               target="view_window">
      ${sourceTitle}: ${object.identifier}
    </html:link>
  </div>


<!-- /pathway.jsp -->