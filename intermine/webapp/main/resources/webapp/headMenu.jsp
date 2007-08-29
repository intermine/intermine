<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- headMenu.jsp -->
<div id="header">
  <tiles:insert name="/header.jsp"/>
  <tiles:insert name="/menu.jsp"/>
</div>

<c:set var="loggedin" value="${!empty PROFILE_MANAGER && !empty PROFILE.username}"/>
<c:set var="itemList" value="bag:lists.upload.tab.title:upload:0 bag:lists.view.tab.title:view:0 mymine:mymine.bags.tab.title:lists:0 mymine:mymine.history.tab.title:history:1 mymine:mymine.savedqueries.tab.title:saved:1 mymine:mymine.savedtemplates.tab.title:templates:1 mymine:mymine.password.tab.title:password:1" />
<fmt:message key="${pageName}.tab" var="tab" />
<tiles:insert name="subMenu.tile">
  <tiles:put name="loggedin" value="${loggedin}"/>
  <tiles:put name="tab" value="${tab}"/>
  <tiles:put name="itemList" value="${itemList}"/>
</tiles:insert>
<!-- Nav trail -->
<div id="navtrail">
  <html:link href="${WEB_PROPERTIES['project.sitePrefix']}"><c:out value="${WEB_PROPERTIES['project.title']}" escapeXml="false"/></html:link>
  <c:if test="${! empty tab}">
    &nbsp;&gt;&nbsp;<html:link action="${tab}"><fmt:message key="menu.${tab}" /></html:link>
  </c:if>
  <c:if test="${pageName != tab}">
    <fmt:message key="${pageName}.title" var="pageTitle">
      <fmt:param value="${param.name}"/>
    </fmt:message>
    &nbsp;&gt;&nbsp;<c:out value="${pageTitle}" />
  </c:if>

  <im:contextHelp/>
  
</div>

<!-- /headMenu.jsp -->
