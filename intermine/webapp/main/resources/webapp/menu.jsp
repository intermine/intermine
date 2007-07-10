<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<!-- menu.jsp -->
<html:xhtml/>
<div id="menu">
  <ul id="nav">
  <li id="home" <c:if test="${pageName=='begin'}">class="activelink"</c:if>>
    <html:link href="${WEB_PROPERTIES['project.sitePrefix']}/">
      <fmt:message key="menu.home"/>
    </html:link>
  </li>
  <li id="bags" <c:if test="${pageName=='bag'}">class="activelink"</c:if>>
    <html:link action="/bag">
      <fmt:message key="menu.bags"/>
    </html:link>
  </li>
  <li id="templates" <c:if test="${pageName=='search'}">class="activelink"</c:if>>
    <html:link action="/search.do?type=template">
      <fmt:message key="menu.templates"/>
    </html:link>
  </li>
  <li <c:if test="${pageName=='query'}">class="activelink"</c:if>>
    <html:link action="/query.do">
      <fmt:message key="menu.querybuilder"/>
    </html:link>
  </li>
  
    <li id="category" <c:if test="${pageName=='category'}">class="activelink"</c:if>>
    <html:link action="/aspects.do">
      <fmt:message key="menu.category"/>
    </html:link>
  </li>
  
  
  <li id="mymine" <c:if test="${pageName=='mymine'}">class="activelink"</c:if>>
    <html:link action="/mymine.do">
      <fmt:message key="menu.mymine"/>
    </html:link>
  </li>
  <%--<li>
    <im:login/>
    &nbsp;
  </li>
  <li>
    <html:link href="${WEB_PROPERTIES['project.sitePrefix']}/doc/manual/">
      <fmt:message key="menu.help"/>
    </html:link>
  </li>--%>
  </ul>
</div>
<script type="text/javascript">
	Nifty("ul#split h3","top transparent");
	// Nifty("ul#split div","bottom");
	Nifty("ul#nav a","small transparent top");
</script>
<div id="quicksearch"><tiles:insert name="browse.tile"> 
  <tiles:put name="menuItem" value="true"/> 
</tiles:insert></div>
<!-- /menu.jsp -->