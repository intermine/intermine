<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>

<!-- menu.jsp -->
<div class="links">
  <p>
    <c:if test="${QUERY != null}">
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
    <span class="menu-item">
      <html:link action="/history.do">
        <fmt:message key="menu.history"/>
      </html:link>
    </span>
    <span class="menu-item">
      <html:link action="/examples.do">
        <fmt:message key="menu.templates"/>
      </html:link>
    </span>
    <span class="menu-item">
      <html:link action="/help.do">
        <fmt:message key="menu.help"/>
      </html:link>
    </span>
  </p>
</div>
<!-- /menu.jsp -->
