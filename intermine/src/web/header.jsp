<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>

<!-- header.jsp -->
<div>
  <div class="headerTitle">
    <html:link href="${WEB_PROPERTIES['project.titleURL']}">
      <c:out value="${WEB_PROPERTIES['project.title']}"/>
    </html:link>
  </div>
  <div class="headerSubtitle">
    <c:out value="${WEB_PROPERTIES['project.subTitle']}"/>
  </div>
</div>
<!-- /header.jsp -->
