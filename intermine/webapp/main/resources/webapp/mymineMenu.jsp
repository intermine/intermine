<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
	
<!-- mymineMenu.jsp -->
	
	<c:set var="MYMINE_PAGE" value="<%=request.getParameter("MYMINE_PAGE")%>"/>
	<c:set var="loggedin" value="<%=request.getParameter("loggedin")%>"/>

	<div id="mymineMenu">
	
	<ul id="mymineList">
	<li>
	<%-- bags --%>
        <c:choose>
          <c:when test="${MYMINE_PAGE=='lists'||MYMINE_PAGE == ''}">
            <span class="active"><fmt:message key="mymine.bags.tab.title"/></span>
          </c:when>
          <c:otherwise>
            <html:link action="/mymine?page=lists">
              <fmt:message key="mymine.bags.tab.title"/>
            </html:link>
          </c:otherwise>
        </c:choose>
    </li>
    <li>&nbsp;|&nbsp;</li>
	
	<li>
	<%-- favourite bags --%>
        <c:choose>
          <c:when test="${MYMINE_PAGE=='favouriteLists'||MYMINE_PAGE == ''}">
            <span class="active"><fmt:message key="mymine.favouriteBags.tab.title"/></span>
          </c:when>
          <c:otherwise>
            <html:link action="/mymine?page=favouriteLists">
              <fmt:message key="mymine.favouriteBags.tab.title"/>
            </html:link>
          </c:otherwise>
        </c:choose>
    </li>
    <li>&nbsp;|&nbsp;</li>
	
    <li>
	<%-- query history --%>
	 <c:choose>
          <c:when test="${MYMINE_PAGE=='history'}">
            <span class="active"><fmt:message key="mymine.history.tab.title"/></span>
          </c:when>
          <c:otherwise>
            <html:link action="/mymine?page=history">
              <fmt:message key="mymine.history.tab.title"/>
            </html:link>
          </c:otherwise>
        </c:choose>
   </li>     
   <li>&nbsp;|&nbsp;</li>
   <li>
	<%-- saved queries --%>
        <c:choose>
          <c:when test="${!loggedin}">
            <span onclick="alert('You need to log in to save queries'); return false;">
              <fmt:message key="mymine.savedqueries.tab.title"/>
            </span>
          </c:when>
          <c:when test="${MYMINE_PAGE=='saved' || !loggedin}">
            <span class="active"><fmt:message key="mymine.savedqueries.tab.title"/></span>
          </c:when>
          <c:otherwise>
            <html:link action="/mymine?page=saved">
              <fmt:message key="mymine.savedqueries.tab.title"/>
            </html:link>
          </c:otherwise>
        </c:choose>
   </li>
   <li>&nbsp;|&nbsp;</li>

   <li>
	<%-- saved templates --%>
        <c:choose>
          <c:when test="${!loggedin}">
            <span onclick="alert('You need to log in to save templates'); return false;">
              <fmt:message key="mymine.savedtemplates.tab.title"/>
            </span>
          </c:when>
          <c:when test="${MYMINE_PAGE=='templates'}">
            <span class="active"><fmt:message key="mymine.savedtemplates.tab.title"/></span>
          </c:when>
          <c:otherwise>
            <html:link action="/mymine?page=templates">
              <fmt:message key="mymine.savedtemplates.tab.title"/>
            </html:link>
          </c:otherwise>
        </c:choose>
  </li>
    
  <li>&nbsp;|&nbsp;</li>
   
   <li>
	<%-- favourites --%>
    
        <c:choose>
          <c:when test="${!loggedin}">
            <span onclick="alert('You need to log in to save favourites'); return false;">
              <fmt:message key="mymine.favourites.tab.title"/>
            </span>
          </c:when>
          <c:when test="${MYMINE_PAGE=='favourites'}">
             <span class="active"><fmt:message key="mymine.favourites.tab.title"/></span>
          </c:when>
          <c:otherwise>
            <html:link action="/mymine?page=favourites">
              <fmt:message key="mymine.favourites.tab.title"/>
            </html:link>
          </c:otherwise>
          
        </c:choose>
 &nbsp;<img src="images/star_active.gif" title="Favourites"/>
    </li>

	<%-- change password --%>

      <c:if test="${loggedin}">
     
      <li>&nbsp;|&nbsp;</li>
          <li>
          <c:choose>
            <c:when test="${MYMINE_PAGE=='password'}">
              <span class="active"><fmt:message key="mymine.password.tab.title"/></span>
            </c:when>
            <c:otherwise>
              <html:link action="/mymine?page=password">
                <fmt:message key="mymine.password.tab.title"/>
              </html:link>
            </c:otherwise>
          </c:choose>
         </li>
      </c:if>
      
	</div>
	<div style="clear:both;"></div>
<!-- /mymineMenu.jsp -->