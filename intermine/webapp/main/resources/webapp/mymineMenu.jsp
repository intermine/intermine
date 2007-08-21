<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
	
<!-- mymineMenu.jsp -->
	
<c:set var="MYMINE_PAGE" value="<%=request.getParameter("page")%>"/>
<c:set var="loggedin" value="<%=request.getParameter("loggedin")%>"/>

<div id="submenu" style="background:#9B89A7;">
  <ul id="submenulist">
    <%-- bags --%>
    <c:choose>
      <c:when test="${MYMINE_PAGE=='lists'||MYMINE_PAGE == null}">
        <li id="activemy"><fmt:message key="mymine.bags.tab.title"/></li>
      </c:when>
      <c:otherwise>
        <li><html:link action="/mymine?page=lists">
          <fmt:message key="mymine.bags.tab.title"/>
        </html:link></li>
      </c:otherwise>
    </c:choose>
    <li>&nbsp;|&nbsp;</li>
    <%-- query history --%>
    <c:choose>
      <c:when test="${MYMINE_PAGE=='history'}">
        <li id="activemy"><fmt:message key="mymine.history.tab.title"/></li>
      </c:when>
      <c:otherwise>
        <li><html:link action="/mymine?page=history">
          <fmt:message key="mymine.history.tab.title"/>
        </html:link></li>
      </c:otherwise>
    </c:choose>
    <li>&nbsp;|&nbsp;</li>
    <%-- saved queries --%>
    <c:choose>
      <c:when test="${!loggedin}">
        <li><span onclick="alert('You need to log in to save queries'); return false;">
          <fmt:message key="mymine.savedqueries.tab.title"/>
        </span></li>
      </c:when>
      <c:when test="${MYMINE_PAGE=='saved' || !loggedin}">
        <li id="activemy"><fmt:message key="mymine.savedqueries.tab.title"/></li>
      </c:when>
      <c:otherwise>
        <li><html:link action="/mymine?page=saved">
          <fmt:message key="mymine.savedqueries.tab.title"/>
        </html:link></li>
      </c:otherwise>
    </c:choose>
    <li>&nbsp;|&nbsp;</li>
    <%-- saved templates --%>
    <c:choose>
      <c:when test="${!loggedin}">
        <li><span onclick="alert('You need to log in to save templates'); return false;">
          <fmt:message key="mymine.savedtemplates.tab.title"/>
        </span></li>
      </c:when>
      <c:when test="${MYMINE_PAGE=='templates'}">
        <li id="activemy"><fmt:message key="mymine.savedtemplates.tab.title"/></li>
      </c:when>
      <c:otherwise>
        <li><html:link action="/mymine?page=templates">
          <fmt:message key="mymine.savedtemplates.tab.title"/>
        </html:link></li>
      </c:otherwise>
    </c:choose>
   <%-- change password --%>
   <c:if test="${loggedin}">
     <li>&nbsp;|&nbsp;</li>
       <c:choose>
         <c:when test="${MYMINE_PAGE=='password'}">
           <li id="activemy"><fmt:message key="mymine.password.tab.title"/></li>
         </c:when>
         <c:otherwise>
           <li><html:link action="/mymine?page=password">
             <fmt:message key="mymine.password.tab.title"/>
           </html:link></li>
         </c:otherwise>
       </c:choose>
     </c:if>
 </ul>
</div>
<div style="clear:both;"></div>

<!-- /mymineMenu.jsp -->