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
    <html:link action="/begin">
      <fmt:message key="menu.begin"/>
    </html:link>
  </li>
  <li id="templates"  <c:if test="${tab == 'templates'}">class="activelink"</c:if>>
     <html:link action="/templates">
      <fmt:message key="menu.templates"/>
    </html:link>
  </li>
  <li id="bags" <c:if test="${tab == 'bag'}">class="activelink"</c:if>>
    <html:link action="/bag">
      <fmt:message key="menu.bag"/>
    </html:link>
  </li>
  <li id="query"  <c:if test="${tab == 'customQuery'}">class="activelink"</c:if>>
    <html:link action="/customQuery">
      <fmt:message key="menu.customQuery"/>&nbsp;
    </html:link>
  </li>
  <li id="category"  <c:if test="${tab == 'dataCategories'}">class="activelink"</c:if>>
    <html:link action="/dataCategories.do">
      <fmt:message key="menu.dataCategories"/>
    </html:link>
  </li>
  <li id="mymine"  <c:if test="${tab == 'mymine'}">class="activelink"</c:if>>
    <html:link action="/mymine.do">
      <fmt:message key="menu.mymine"/>
    </html:link>
  </li>
  </ul>

<!-- /menu.jsp -->