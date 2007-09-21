<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- diseaseLong.jsp -->

<fmt:setBundle basename="model"/>

<div style="margin-left: 20px">
  <c:set var="sourceTitle" value="OMIM"/>
  <c:set var="linkProperty" value="${sourceTitle}.url.prefix"/>
  
  <html:link href="${WEB_PROPERTIES[linkProperty]}${object.omimId}"
             title="${sourceTitle}: ${object.omimId}"
             target="view_window">
    <html:img src="model/images/${sourceTitle}_logo_small.png" align="middle" border="0"/>
    ${sourceTitle}: ${object.omimId}
  </html:link>
</div>

<!-- /diseaseLong.jsp -->
