<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<!-- menu.jsp -->
<html:xhtml/>
<tr><td id="menu" colspan="2">
  <ul id="nav">
  <li id="home" <c:if test="${pageName=='begin'}">class="activelink"</c:if>>
    <html:link action="/begin">
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
  <li id="query" <c:if test="${pageName=='customQuery'}">class="activelink"</c:if>>
    <html:link action="/customQuery">
      <fmt:message key="menu.querybuilder"/>&nbsp;
    </html:link>
  </li>
  <li id="category" <c:if test="${pageName=='aspects'}">class="activelink"</c:if>>
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
</td></tr>
<tr><td id="quicksearch" colspan="2"><tiles:insert name="browse.tile"> 
  <tiles:put name="menuItem" value="true"/> 
</tiles:insert></td></tr></table>
<script type="text/javascript">
	Nifty("ul#nav a","transparent top");
</script>

<!-- /menu.jsp -->