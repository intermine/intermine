<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- header.jsp -->

<div id="topright">
  <div class="wellcome">FlyMine is funded by <a href="http://www.wellcome.ac.uk/">The Wellcome Trust</a>.</div>
  <div class="contact">
    University of Cambridge - UK<br/>
    Tel: +44 (0)1223 333377<br/>
    <a href="mailto:info%5Bat%5Dflymine.org">info[at]flymine.org</a>
  </div>
</div>

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
