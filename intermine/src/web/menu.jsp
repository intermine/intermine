<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>

<!-- menu.jsp -->
<div class="links">
  <p>
    <span class="menu-item"><html:link href="/index"><bean:message key="menu.about"/></html:link></span>
    <span class="menu-item"><a href="/team"><fmt:message key="menu.team"/></a></span>
    <span class="menu-item"><a href="/funding"><fmt:message key="menu.funding"/></a></span>
    <span class="menu-item"><a href="/software"><fmt:message key="menu.software"/></a></span>
    <span class="menu-item"><a href="/database"><fmt:message key="menu.database"/></a></span>
    <span class="menu-item"><a href="/api"><fmt:message key="menu.javadoc"/></a></span>
  </p>
</div>
<!-- /menu.jsp -->
