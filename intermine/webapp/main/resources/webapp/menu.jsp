<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<!-- menu.jsp -->
<html:xhtml/>

  <fmt:message key="${pageName}.tab" var="tab" />

<ul id="nav">
  <li id="home" <c:if test="${tab == 'home'}">class="activelink"</c:if>>
    <html:link action="/begin">
      <fmt:message key="menu.home"/>
    </html:link>
  </li>
  <li id="templates"  <c:if test="${tab == 'templates'}">class="activelink"</c:if>>
     <html:link action="/templates">
      <fmt:message key="menu.templates"/>
    </html:link>
  </li>
  <li id="bags" <c:if test="${tab == 'bags'}">class="activelink"</c:if>>
    <html:link action="/bag">
      <fmt:message key="menu.bags"/>
    </html:link>
  </li>
  <li id="query"  <c:if test="${tab == 'query'}">class="activelink"</c:if>>
    <html:link action="/customQuery">
      <fmt:message key="menu.querybuilder"/>&nbsp;
    </html:link>
  </li>
  <li id="category"  <c:if test="${tab == 'data'}">class="activelink"</c:if>>
    <html:link action="/dataCategories.do">
      <fmt:message key="menu.category"/>
    </html:link>
  </li>
  <li id="mymine"  <c:if test="${tab == 'mymine'}">class="activelink"</c:if>>
    <html:link action="/mymine.do">
      <fmt:message key="menu.mymine"/>
    </html:link>
  </li>
  </ul>

<c:set var="loggedin" value="${!empty PROFILE_MANAGER && !empty PROFILE.username}"/>
<c:set var="itemList" value="bags:lists.upload.tab.title:upload:0 bags:lists.view.tab.title:view:0 mymine:mymine.bags.tab.title:lists:0 mymine:mymine.history.tab.title:history:1 mymine:mymine.savedqueries.tab.title:saved:1 mymine:mymine.savedtemplates.tab.title:templates:1 mymine:mymine.password.tab.title:password:1" />
<tiles:insert name="subMenu.tile" >
  <tiles:put name="loggedin" value="${loggedin}"/>
  <tiles:put name="tab" value="${tab}"/>
  <tiles:put name="itemList" value="${itemList}"/>
</tiles:insert>

<!-- /menu.jsp -->