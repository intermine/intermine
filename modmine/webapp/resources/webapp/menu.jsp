<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<!-- menu.jsp -->
<html:xhtml/>

  <fmt:message key="${pageName}.tab" var="tab" />

<ul id="nav">
  <li id="home" <c:if test="${tab == 'begin'}">class="activelink"</c:if>>
    <a href="/${WEB_PROPERTIES['webapp.path']}/begin.do">
      <fmt:message key="menu.begin"/>
    </a>
  </li>
  <li id="templates"  <c:if test="${tab == 'templates'}">class="activelink"</c:if>>
     <a href="/${WEB_PROPERTIES['webapp.path']}/templates.do">
      <fmt:message key="menu.templates"/>
    </a>
  </li>
  <li id="bags" <c:if test="${tab == 'bag'}">class="activelink"</c:if>>
    <a href="/${WEB_PROPERTIES['webapp.path']}/bag.do">
      <fmt:message key="menu.bag"/>
    </a>
  </li>
  <li id="query"  <c:if test="${tab == 'customQuery'}">class="activelink"</c:if>>
    <a href="/${WEB_PROPERTIES['webapp.path']}/customQuery.do">
      <fmt:message key="menu.customQuery"/>&nbsp;
    </a>
  </li>
  <li id="project"  <c:if test="${tab == 'modEncodeProjects'}">class="activelink"</c:if>>
    <a href="/${WEB_PROPERTIES['webapp.path']}/modEncodeProjects.do">
      <fmt:message key="menu.modEncodeProjects"/>
    </a>
  </li>
  <li id="category"  <c:if test="${tab == 'dataCategories'}">class="activelink"</c:if>>
    <a href="/${WEB_PROPERTIES['webapp.path']}/dataCategories.do">
      <fmt:message key="menu.dataCategories"/>
    </a>
  </li>
  <li id="mymine"  <c:if test="${tab == 'mymine'}">class="activelink"</c:if>>
    <a href="/${WEB_PROPERTIES['webapp.path']}/mymine.do">
      <fmt:message key="menu.mymine"/>
    </a>
  </li>
  </ul>

<!-- /menu.jsp -->