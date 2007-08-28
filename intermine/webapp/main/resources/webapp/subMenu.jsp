<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<tiles:useAttribute name="subtab" id="subtab"/>
<tiles:useAttribute name="tab" id="tab"/>
<tiles:useAttribute name="loggedin" id="loggedin"/>
<tiles:useAttribute name="itemList" id="itemList"/>

<!-- mymineMenu.jsp -->
<div style="clear:both;"></div>
<c:choose>
  <c:when test="${tab == 'mymine'}">
    <c:set var="styleClass" value="submenu_mymine" />
  </c:when>
  <c:otherwise>
    <c:set var="styleClass" value="submenu" />
  </c:otherwise>
</c:choose>
<div id="submenu" class="${styleClass}">

<div id="quicksearch">
  <tiles:insert name="browse.tile"> 
    <tiles:put name="menuItem" value="true"/> 
  </tiles:insert>
</div>

<ul id="submenulist">
<c:set var="count" value="0"/>
<c:forTokens items="${itemList}" delims=" " var="item" varStatus="counter">
  <c:set var="tabArray" value="${fn:split(item, ':')}" />
  <c:if test="${tabArray[0] == tab}">
  <c:if test="${count>0}">
    <li>&nbsp;|&nbsp;</li>
  </c:if>
  <c:choose>
    <c:when test="${(empty subtab && count == 0)||(subtab == tabArray[2])}">
      <li id="subactive_${tab}"><fmt:message key="${tabArray[1]}" /></li>
    </c:when>
    <c:when test="${(tabArray[3] == '1') && (loggedin == false)}">
      <li>
        <span onclick="alert('You need to log in'); return false;"><fmt:message key="${tabArray[1]}"/></span>
      </li>
    </c:when>
    <c:otherwise>
      <li><html:link action="${pageName}?subtab=${tabArray[2]}"><fmt:message key="${tabArray[1]}"/></html:link></li>
    </c:otherwise>
  </c:choose>
  <c:set var="count" value="${count+1}"/>
  </c:if>
</c:forTokens>
  
</ul>

</div>
<div style="clear:both;"></div>

<!-- /mymineMenu.jsp -->