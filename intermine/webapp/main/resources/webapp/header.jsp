<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- header.jsp -->

<div id="header">
  <div id="topright">
    <span class="version"><fmt:message key="header.version"/> <c:out value="${WEB_PROPERTIES['project.releaseVersion']}" escapeXml="false"/></span><br/>
    <div class="contact">${WEB_PROPERTIES['project.contact']}</div>
    <div class="wellcome">${WEB_PROPERTIES['project.funded.by']}</div>
  </div>
  
  <html:link href="${WEB_PROPERTIES['project.sitePrefix']}/">
    <img src="model/logo.png" border="0" id="logo"/>
  </html:link>
  <div id="title">
    <h1>
      <html:link href="${WEB_PROPERTIES['project.sitePrefix']}/">
        <c:out value="${WEB_PROPERTIES['project.title']}" escapeXml="false"/>
      </html:link>
    </h1>
    <p>
      <c:out value="${WEB_PROPERTIES['project.subTitle']}" escapeXml="false"/>
    </p>
  </div>
  <div class="clear-both"></div>
</div>

<!-- /header.jsp -->
