<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>

<!-- header.jsp -->
<div id="header">
  <div id="headerTitle">
    <html:link href="${WEB_PROPERTIES['project.titleURL']}">
      <c:out value="${WEB_PROPERTIES['project.title']}" escapeXml="false"/>
    </html:link>
  </div>
  <div id="headerSubtitle">
    <c:out value="${WEB_PROPERTIES['project.subTitle']}" escapeXml="false"/>
  </div>
</div>
<!-- /header.jsp -->
