<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<p>
  <font class="menu-heading"><bean:message key="menu.project"/></font><br />

  <font class="menu-item"><a href="/index"><bean:message key="menu.about"/></a></font><br />
  <font class="menu-item"><a href="/team"><bean:message key="menu.team"/></a></font><br />
  <font class="menu-item"><a href="/funding"><bean:message key="menu.funding"/></a></font><br />
  <font class="menu-item"><a href="/presentations"><bean:message key="menu.presentations"/></a></font><br />
</p>
<p>
  <font class="menu-heading"><bean:message key="menu.documentation"/></font><br />

  <font class="menu-item"><a href="/software"><bean:message key="menu.software"/></a></font><br />
  <font class="menu-item"><a href="/database"><bean:message key="menu.database"/></a></font><br />
  <font class="menu-item"><a href="/api"><bean:message key="menu.javadoc"/></a></font><br />
</p>
<p>
  <font class="menu-heading"><bean:message key="menu.download"/></font><br />

  <font class="menu-item"><a href="/index"><bean:message key="menu.latest"/></a></font><br />
  <font class="menu-item"><a href="/team"><bean:message key="menu.nightly"/></a></font><br />
</p>
<p>
  <font class="menu-heading"><bean:message key="menu.cvs"/></font><br />

  <font class="menu-item"><a href="http://cvs.flymine.org"><bean:message key="menu.cvsbrowse"/></a></font><br />
  <font class="menu-item"><a href="/software/cvs-user"><bean:message key="menu.cvsanonymous"/></a></font><br />
</p>
<p>
  <font class="menu-heading"><a href="http://mailman.flymine.org/listinfo/"><bean:message key="menu.mailinglists"/></a></font><br />
</p>

<p>
    <font class="menu-heading"><a href="/internal/index"><bean:message key="menu.internal"/></a></font><br />
</p>

