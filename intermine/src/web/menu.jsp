<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>

<!-- menu.jsp -->
<p>
  <font class="menu-heading">
    <fmt:message key="menu.project"/>
  </font>
  <br/>
  <font class="menu-item">
    <a href="/index">
      <fmt:message key="menu.about"/>
    </a>
  </font>
  <br/>
  <font class="menu-item">
    <a href="/team">
      <fmt:message key="menu.team"/>
    </a>
  </font>
  <br/>
  <font class="menu-item">
    <a href="/funding">
      <fmt:message key="menu.funding"/>
    </a>
  </font>
  <br/>
  <font class="menu-item">
    <a href="/presentations">
      <fmt:message key="menu.presentations"/>
    </a>
  </font>
  <br/>
</p>

<p>
  <font class="menu-heading">
    <fmt:message key="menu.documentation"/>
  </font>
  <br/>
  <font class="menu-item">
    <a href="/software"><fmt:message key="menu.software"/></a>
  </font>
  <br/>
  <font class="menu-item">
    <a href="/database"><fmt:message key="menu.database"/></a>
  </font>
  <br/>
  <font class="menu-item">
    <a href="/api"><fmt:message key="menu.javadoc"/></a>
  </font>
  <br/>
</p>

<p>
  <font class="menu-heading">
    <fmt:message key="menu.download"/>
  </font>
  <br/>
  <font class="menu-item">
    <a href="/index"><fmt:message key="menu.latest"/></a>
  </font>
  <br/>
  <font class="menu-item">
    <a href="/team"><fmt:message key="menu.nightly"/></a>
  </font>
  <br/>
</p>

<p>
  <font class="menu-heading">
    <fmt:message key="menu.cvs"/>
  </font>
  <br/>
  <font class="menu-item">
    <a href="http://cvs.flymine.org"><fmt:message key="menu.cvsbrowse"/></a>
  </font>
  <br/>
  <font class="menu-item">
    <a href="/software/cvs-user"><fmt:message key="menu.cvsanonymous"/></a>
  </font><br/>
</p>

<p>
  <font class="menu-heading">
    <a href="http://mailman.flymine.org/listinfo/"><fmt:message key="menu.mailinglists"/></a>
  </font>
  <br/>
</p>

<p>
  <font class="menu-heading">
    <a href="/internal/index"><fmt:message key="menu.internal"/></a>
  </font>
  <br/>
</p>
<!-- /menu.jsp -->
