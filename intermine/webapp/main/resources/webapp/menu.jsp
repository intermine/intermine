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
  <div id="quicksearch">
    <tiles:insert name="browse.tile"> 
      <tiles:put name="menuItem" value="true"/> 
    </tiles:insert>
  </div>
  
<div style="clear:both;"></div>

<c:if test="${tab == 'mymine'}">
 <c:set var="loggedin" value="${!empty PROFILE_MANAGER && !empty PROFILE.username}"/>
  <jsp:include page="mymineMenu.jsp" flush="true">
    <jsp:param name="loggedin" value="${loggedin}"/>  
  </jsp:include>
</c:if>
<c:if test="${tab == 'bags'}">
<div id="submenu" style="background:#A42F2D">
  <c:set var="page" value="<%=request.getParameter("page")%>"/>
  <ul id="submenulist">
      <c:choose>
      <c:when test="${empty page || page == 'create'}">
        <li id="activelist">Create</li>
      </c:when>
      <c:otherwise>
        <li><html:link action="bag.do?page=create">Create</html:link></li>
      </c:otherwise>
      </c:choose>
    <li>&nbsp;|&nbsp;</li>
    <li>
    <c:choose>
    <c:when test="${page == 'view'}">
      <li id="activelist">View</li>
    </c:when>
    <c:otherwise>
      <li><html:link action="bag.do?page=view">View</html:link></li>
    </c:otherwise>
    </c:choose>
  </ul>
</div>
<div style="clear:both"></div>
</c:if>
<!-- /menu.jsp -->