<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- headMenu.jsp -->
<html:xhtml/>

<tiles:importAttribute name="fixedLayout" ignore="true" />

<!-- Header container -->
<div align="center" id="headercontainer">

  <!-- Header -->
  <c:set value="${WEB_PROPERTIES['header.links']}" var="headerLinks"/>

  <c:if test="${fn:length(headerLinks) > 0}">
    <%-- Menu appearing at the top right (about, etc..) --%>
    <div id="topnav">
      <c:forEach var="entry" items="${headerLinks}" varStatus="status">
        <c:if test="${status.count != 1}">&nbsp;|&nbsp;</c:if>
        <c:set value="header.links.${entry}" var="linkProp"/>
        <c:choose>
          <c:when test="${!empty WEB_PROPERTIES[linkProp]}">
                  <a href="${WEB_PROPERTIES[linkProp]}">${entry}</a>
          </c:when>
          <c:otherwise>
            <a href="${WEB_PROPERTIES['project.sitePrefix']}/${entry}.shtml">${entry}</a>
          </c:otherwise>
        </c:choose>
      </c:forEach>
    </div>
  </c:if>
  <div id="imheader">
    <!--<a href="${WEB_PROPERTIES['project.sitePrefix']}" alt="Home" rel="NOFOLLOW"><img id="logo" src="model/images/logo.png" width="45px" height="43px" alt="Logo" /></a>-->
    <a href="${WEB_PROPERTIES['webapp.wormbase.url']}" alt="Home" rel="NOFOLLOW"><img id="logo" src="model/images/logo_wb.png" height="34px" alt="Logo" /></a>
    <h1><html:link href="/${WEB_PROPERTIES['webapp.path']}/"><c:out value="${WEB_PROPERTIES['project.title']}" escapeXml="false"/></html:link></h1>
    <p id="version"><fmt:message key="header.version"/> <c:out value="${WEB_PROPERTIES['project.releaseVersion']}" escapeXml="false"/></span>
    <p><c:out value="${WEB_PROPERTIES['project.subTitle']}" escapeXml="false"/></p>
  </div>

<!-- beta notice-->
<!--<p id="beta-notice"
   style="text-align: left;
          width: 800px;
	  margin: -1em 2em 1em;
	  color: #535353;">Disclaimer: This is a beta release of WormMine WS${WEB_PROPERTIES['project.releaseVersion']}. Although we have done our best to ensure the accuracy of this release, please <a href="/tools/support">contact us</a> if you discover problems with data output or functionality.</p>-->

    <!-- Tab Menu -->
  <fmt:message key="${pageName}.tab" var="tab" />
  <div id="menucontainer">
    <ul id="nav">
      <li id="home" <c:if test="${tab == 'begin'}">class="activelink"</c:if>>
        <a href="/${WEB_PROPERTIES['webapp.path']}/begin.do">
          <fmt:message key="menu.begin"/>
        </a>
      </li>
      <li id="templates"  <c:if test="${tab == 'templates'}">class="activelink"</c:if>>
         <a href="/${WEB_PROPERTIES['webapp.path']}/templates.do">
          <fmt:message key="menu.templates"/>
        </a>
      </li>
      <li id="bags" <c:if test="${tab == 'bag'}">class="activelink"</c:if>>
        <a href="/${WEB_PROPERTIES['webapp.path']}/bag.do">
          <fmt:message key="menu.bag"/>
        </a>
      </li>
      <li id="query" <c:if test="${tab == 'customQuery'}">class="activelink"</c:if>>
        <a href="/${WEB_PROPERTIES['webapp.path']}/customQuery.do">
          <fmt:message key="menu.customQuery"/>&nbsp;
        </a>
      </li>
      <!--
      <c:if test="${WEB_PROPERTIES['genomicRegionSearch.display'] == 'true'}">
          <li id="genomicRegionSearch" <c:if test="${tab == 'genomicRegionSearch'}">class="activelink"</c:if>>
            <a href="/${WEB_PROPERTIES['webapp.path']}/genomicRegionSearch.do">
              <fmt:message key="menu.genomicRegionSearch"/>
            </a>
          </li>
      </c:if>
      <li id="category" <c:if test="${tab == 'dataCategories'}">class="activelink"</c:if>>
        <a href="/${WEB_PROPERTIES['webapp.path']}/dataCategories.do">
          <fmt:message key="menu.dataCategories"/>
        </a>
      </li>
      -->
      <li id="api"  <c:if test="${tab == 'api'}">class="activelink"</c:if>>
        <a href="/${WEB_PROPERTIES['webapp.path']}/api.do">
          <fmt:message key="menu.api"/>
        </a>
      </li>
      <li id="mymine"  <c:if test="${tab == 'mymine'}">class="activelink"</c:if>>
        <a href="/${WEB_PROPERTIES['webapp.path']}/mymine.do">
          <span><fmt:message key="menu.mymine"/></span>
        </a>
      </li>
    </ul>
  <ul id="loginbar">
	<!--
        <li><a href="#" onclick="showContactForm();return false;"><fmt:message key="feedback.link"/></a></li>
	-->
        <c:if test="${PROFILE.loggedIn}">
            <li>

              <!-- display (optionally trimmed) username -->
              <c:choose>
                <c:when test="${! empty PROVIDER}">
                  <c:choose>
                    <c:when test="${empty USERNAME || USERNAME == 'nullnull'}">
                      <c:set var="displayUserName" value="logged in with OpenID"/>
                    </c:when>
                   <c:otherwise>
                     <c:set var="displayUserName" value="${USERNAME}"/>
                   </c:otherwise>
                  </c:choose>
                </c:when>
                <c:otherwise>
                  <c:set var="displayUserName" value="${PROFILE.name}"/>
                </c:otherwise>
              </c:choose>

              <c:choose>
                <c:when test="${fn:length(displayUserName) > 25}">
                  <c:out value="${fn:substring(displayUserName,0,25)}"/>&hellip;
                </c:when>
                <c:otherwise>
                  <c:out value="${displayUserName}"/>
                </c:otherwise>
              </c:choose>
            </li>
        </c:if>
        <li class="last"><im:login/></li>
    </ul>
  </div>

  <!-- Logged in section -->
  <c:set var="loggedin" value="${PROFILE.loggedIn}"/>

  <!-- Submenu section -->
  <c:set var="itemList" value="bag:lists.upload.tab.title:upload:0 bag:lists.view.tab.title:view:0 api:api.perl.tab.title:perl:0 api:api.python.tab.title:python:0 api:api.ruby.tab.title:ruby:0 api:api.java.tab.title:java:0 mymine:mymine.bags.tab.title:lists:0 mymine:mymine.history.tab.title:history:0 mymine:mymine.savedqueries.tab.title:saved:1 mymine:mymine.savedtemplates.tab.title:templates:1" />
   <c:if test="${PROFILE.superuser}">
       <c:set var="itemList" value="${itemList} mymine:mymine.tracks.tab.title:tracks:1 mymine:mymine.users.tab.title:users:1 mymine:mymine.labels.tab.title:labels:0"></c:set>
   </c:if>
   <c:if test="${PROFILE.local}">
       <c:set var="itemList" value="${itemList} mymine:mymine.password.tab.title:password:1"/>
   </c:if>
    <c:set var="itemList" value="${itemList} mymine:mymine.account.tab.title:account:1"/>
  <fmt:message key="${pageName}.tab" var="tab" />
  <c:choose>
    <c:when test="${tab == 'mymine'}">
      <c:set var="styleClass" value="submenu_mymine" />
    </c:when>
    <c:otherwise>
      <c:set var="styleClass" value="submenu" />
    </c:otherwise>
  </c:choose>
  <c:set var="submenuid" value="submenu"/>
  <c:if test="${fixedLayout == true}">
    <c:set var="submenuid" value="${submenuid}fixed"/>
  </c:if>
  <div id="${submenuid}" class="${styleClass}">
    <div id="submenudiv">
      <div id="quicksearch">
        <tiles:insert name="quickSearch.tile">
          <tiles:put name="menuItem" value="true"/>
        </tiles:insert>
      </div>
        <ul id="submenulist">
        <c:set var="count" value="0"/>
        <c:set var="subtabName" value="subtab${pageName}" scope="request" />
        <c:forTokens items="${itemList}" delims=" " var="item" varStatus="counter">
          <c:set var="tabArray" value="${fn:split(item, ':')}" />
          <c:if test="${tabArray[0] == tab}">
          <c:choose>
            <c:when test="${((empty subtabs[subtabName] && count == 0)||(subtabs[subtabName] == tabArray[2])) && (tab == pageName)}">
              <%-- open li element --%>
        <li id="subactive_${tab}"
                <c:choose>
                  <c:when test="${count == 0}">class="first ${fn:replace(tabArray[1], ".", "")}"</c:when>
                  <c:otherwise>class="${fn:replace(tabArray[1], ".", "")}"</c:otherwise>
                </c:choose>
              > <%-- Close li element --%>
                <div><span><fmt:message key="${tabArray[1]}" /></span></div>
              </li>
            </c:when>
            <c:when test="${(tabArray[3] == '1') && (loggedin == false)}">
              <%-- open li --%>
              <li
                <c:choose>
                  <c:when test="${count == 0}">class="first ${fn:replace(tabArray[1], ".", "")}"</c:when>
                  <c:otherwise>class="${fn:replace(tabArray[1], ".", "")}"</c:otherwise>
                </c:choose>
        >
        <%-- close li --%>
        <div>
                <span onclick="alert('You need to log in'); return false;">
                  <fmt:message key="${tabArray[1]}"/>
                </span>
                </div>
              </li>
            </c:when>
            <c:otherwise>
              <%-- open li --%>
              <li
                <c:choose>
                  <c:when test="${count == 0}">class="first ${fn:replace(tabArray[1], ".", "")}"</c:when>
                  <c:otherwise>class="${fn:replace(tabArray[1], ".", "")}"</c:otherwise>
                </c:choose>
        >
        <%-- close li --%>
                <div>
                <a href="/${WEB_PROPERTIES['webapp.path']}/${tab}.do?subtab=${tabArray[2]}">
                  <fmt:message key="${tabArray[1]}"/>
                </a>
                </div>
              </li>
            </c:otherwise>
          </c:choose>
          <c:set var="count" value="${count+1}"/>
          </c:if>
        </c:forTokens>
        <!--
        <c:if test="${pageName == 'begin'}">
          <li>
          <div>
            <a href="${WEB_PROPERTIES['project.sitePrefix']}/what.shtml">What is ${WEB_PROPERTIES['project.title']}?</a>
          </div>
          </li>
        </c:if>
         -->
        </ul>
    </div>
  </div>

</div>
<!-- /headMenu.jsp -->
