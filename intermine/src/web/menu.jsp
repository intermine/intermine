<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- menu.jsp -->
<html:xhtml/>
<div class="links">
  <span class="menu-item">
    <html:link href="${WEB_PROPERTIES['project.sitePrefix']}/">
      <fmt:message key="menu.home"/>
    </html:link>
  </span>
  <c:if test="${!empty QUERY}">
    <span class="menu-item">
      <html:link action="/query.do">
        <fmt:message key="menu.currentquery"/>
      </html:link>
    </span>
  </c:if>
  <span class="menu-item">
    <html:link action="/begin.do">
      <fmt:message key="menu.newquery"/>
    </html:link>
  </span>
  <c:if test="${!empty PROFILE.savedBags || !empty PROFILE.savedQueries}">
    <span class="menu-item">
      <html:link action="/history.do">
        <fmt:message key="menu.history"/>
      </html:link>
    </span>
  </c:if>
  <span class="menu-item">
    <html:link action="/examples.do">
      <fmt:message key="menu.examples"/>
    </html:link>
  </span>
  <span class="menu-item">
    <html:link action="/templates.do">
      <fmt:message key="menu.templates"/>
    </html:link>
  </span>
  <c:if test="${!empty PROFILE_MANAGER && empty PROFILE.username}">
    <span class="menu-item">
      <html:link action="/login.do">
        <fmt:message key="menu.login"/>
      </html:link>
    </span>
  </c:if>
  <span class="menu-item">
    <html:link action="/feedback.do">
      <fmt:message key="menu.feedback"/>
    </html:link>
  </span>
  <span class="menu-item">
    <html:link href="${WEB_PROPERTIES['project.sitePrefix']}/doc/manual/index.html">
      <fmt:message key="menu.help"/>
    </html:link>
  </span>
</div>

<!-- /menu.jsp -->
