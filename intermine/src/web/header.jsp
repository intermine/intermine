<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- header.jsp -->
<div id="header">
  <h1>
    <html:link href="${WEB_PROPERTIES['project.sitePrefix']}/">
      <c:out value="${WEB_PROPERTIES['project.title']}" escapeXml="false"/>
    </html:link>
  </h1>
  <p>
    <c:out value="${WEB_PROPERTIES['project.subTitle']}" escapeXml="false"/>
  </p>
</div>

<!-- /header.jsp -->
