<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
	
<!-- mymineMenu.jsp -->
	
	<c:set var="MYMINE_PAGE" value="<%=request.getParameter("MYMINE_PAGE")%>"/>
	<c:set var="loggedin" value="<%=request.getParameter("loggedin")%>"/>

	<div align="center" style="font-size:+1.2em;color:#36c">
	
	<table cellspacing="0" cellpadding="2">
	<tr>
	<td valign="top"><b>MyMine Menu</b>&nbsp;</td>
	<td style="border-bottom: #36c dashed 1.3px;">
	
	
	<%-- bags --%>
        <c:choose>
          <c:when test="${MYMINE_PAGE=='lists'||MYMINE_PAGE == ''}">
            <b><fmt:message key="mymine.bags.tab.title"/></b>
          </c:when>
          <c:otherwise>
            <html:link action="/mymine?page=lists">
              <fmt:message key="mymine.bags.tab.title"/>
            </html:link>
          </c:otherwise>
        </c:choose>
    
    &nbsp;|&nbsp;
	
	<%-- query history --%>
	 <c:choose>
          <c:when test="${MYMINE_PAGE=='history'}">
            <b><fmt:message key="mymine.history.tab.title"/></b>
          </c:when>
          <c:otherwise>
            <html:link action="/mymine?page=history">
              <fmt:message key="mymine.history.tab.title"/>
            </html:link>
          </c:otherwise>
        </c:choose>
        
	&nbsp;|&nbsp;

	<%-- saved queries --%>
        <c:choose>
          <c:when test="${!loggedin}">
            <span onclick="alert('You need to log in to save queries'); return false;">
              <fmt:message key="mymine.savedqueries.tab.title"/>
            </span>
          </c:when>
          <c:when test="${MYMINE_PAGE=='saved' || !loggedin}">
            <b><fmt:message key="mymine.savedqueries.tab.title"/></b>
          </c:when>
          <c:otherwise>
            <html:link action="/mymine?page=saved">
              <fmt:message key="mymine.savedqueries.tab.title"/>
            </html:link>
          </c:otherwise>
        </c:choose>
       	
	&nbsp;|&nbsp;

	<%-- saved templates --%>
        <c:choose>
          <c:when test="${!loggedin}">
            <span onclick="alert('You need to log in to save templates'); return false;">
              <fmt:message key="mymine.savedtemplates.tab.title"/>
            </span>
          </c:when>
          <c:when test="${MYMINE_PAGE=='templates'}">
            <b><fmt:message key="mymine.savedtemplates.tab.title"/></b>
          </c:when>
          <c:otherwise>
            <html:link action="/mymine?page=templates">
              <fmt:message key="mymine.savedtemplates.tab.title"/>
            </html:link>
          </c:otherwise>
        </c:choose>

    
    &nbsp;|&nbsp;

	<%-- favourites --%>
    
        <c:choose>
          <c:when test="${!loggedin}">
            <span onclick="alert('You need to log in to save favourites'); return false;">
              <fmt:message key="mymine.favourites.tab.title"/>
            </span>
          </c:when>
          <c:when test="${MYMINE_PAGE=='favourites'}">
             <b><fmt:message key="mymine.favourites.tab.title"/></b>
          </c:when>
          <c:otherwise>
            <html:link action="/mymine?page=favourites">
              <fmt:message key="mymine.favourites.tab.title"/>
            </html:link>
          </c:otherwise>
          
        </c:choose>
 &nbsp;<img src="images/star_active.gif" title="Favourites"/>



	<%-- change password --%>

      <c:if test="${loggedin}">
     
          &nbsp;|&nbsp;
          
          <c:choose>
            <c:when test="${MYMINE_PAGE=='password'}">
              <b><fmt:message key="mymine.password.tab.title"/></b>
            </c:when>
            <c:otherwise>
              <html:link action="/mymine?page=password">
                <fmt:message key="mymine.password.tab.title"/>
              </html:link>
            </c:otherwise>
          </c:choose>

      </c:if>
      
      </td>     
      </tr>
      <!--<tr><td><hr style="width:60%;align:center;background-color:#36c;color:#36c;height:5px;border: 0;"/></td></tr>-->
      </table>
	</div>
	
	
	<br/>
	
<!-- /mymineMenu.jsp -->