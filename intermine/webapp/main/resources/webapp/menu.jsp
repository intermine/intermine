<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<!-- menu.jsp -->
<html:xhtml/>
<tr><td id="menu" colspan="3">
  <ul id="nav">
  <li id="home">
    <html:link action="/begin">
      <fmt:message key="menu.home"/>
    </html:link>
  </li>
  <li id="bags">
    <html:link action="/bag">
      <fmt:message key="menu.bags"/>
    </html:link>
  </li>
  <li id="templates">
    <html:link action="/search.do?type=template">
      <fmt:message key="menu.templates"/>
    </html:link>
  </li>
  <li id="query">
    <html:link action="/customQuery">
      <fmt:message key="menu.querybuilder"/>&nbsp;
    </html:link>
  </li>
  <li id="category">
    <html:link action="/aspects.do">
      <fmt:message key="menu.category"/>
    </html:link>
  </li>
  <li id="mymine">
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
  </td>
</tr>
<tr>
  <td height="10px" class="${pageName}_background" colspan="3">
    <%-- empty td for formatting purposes --%>
    &nbsp;
  </td>
<tr>
  <td colspan="2" width="66%" align="left" nowrap>
    <div class="${pageName}_background">
      <tiles:insert name="browse.tile"> 
        <tiles:put name="menuItem" value="true"/> 
      </tiles:insert>
    </div>
  </td>
  <td class="${pageName}_background" width="33%" align="right">
    <c:if test="${!shownAspectsPopup}">
      <tiles:insert page="/aspectPopup.jsp"/>
      <c:set scope="request" var="shownAspectsPopup" value="${true}"/>
    </c:if>
  </td>  
</tr>

<script type="text/javascript">
	Nifty("ul#nav a","transparent top");
</script>

<!-- /menu.jsp -->
