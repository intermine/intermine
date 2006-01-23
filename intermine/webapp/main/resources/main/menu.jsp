<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- menu.jsp -->
<html:xhtml/>
<div class="links">
  <c:if test="${!empty PROFILE.username}">
    <span class="menu-logged-in-item">
      ${PROFILE.username}
    </span>
  </c:if>
  <span class="menu-item">
    <html:link href="${WEB_PROPERTIES['project.sitePrefix']}/">
      <fmt:message key="menu.home"/>
    </html:link>
  </span>
  <span class="menu-item">
    <html:link action="/begin.do">
      <fmt:message key="menu.newquery"/>
    </html:link>
  </span>
  <span class="menu-item">
    <c:choose>
      <c:when test="${!empty QUERY}">
        <html:link action="/query.do">
          <fmt:message key="menu.currentquery"/>
        </html:link>
      </c:when>
      <c:otherwise>
        <fmt:message key="menu.currentquery"/>
      </c:otherwise>
    </c:choose>
  </span>
  <span class="menu-item">
    <html:link action="/history.do">
      <fmt:message key="menu.history"/>
    </html:link>
  </span>
  <span class="menu-item">
    <html:link action="/examples.do">
      <fmt:message key="menu.examples"/>
    </html:link>
  </span>
  <span class="menu-item">
    <html:link action="/templateSearch.do">
      <fmt:message key="menu.searchTemplates"/>
    </html:link>
    <img src="images/inspect.gif" width="12" height="11" alt="-&gt;"/>
  </span>
  <span class="menu-item">
    <html:link action="/feedback.do">
      <fmt:message key="menu.feedback"/>
    </html:link>
  </span>
  <span class="menu-item">
    <c:choose>
      <c:when test="${!empty PROFILE_MANAGER && empty PROFILE.username}">
        <html:link action="/login.do">
          <fmt:message key="menu.login"/>
        </html:link>
      </c:when>
      <c:otherwise>
        <html:link action="/logout.do">
          <fmt:message key="menu.logout"/>
        </html:link>
      </c:otherwise>
    </c:choose>
  </span>
  <span class="menu-item">
    <html:link href="${WEB_PROPERTIES['project.sitePrefix']}/doc/manual/index.html">
      <fmt:message key="menu.help"/>
    </html:link>
  </span>
  
</div>

<!-- /menu.jsp -->
