<%@ tag body-content="scriptless" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ attribute name="pageName" required="true" %>
<%@ attribute name="helpimg" required="false" %>
<%@ attribute name="anchor" required="false" %>

<%-- outputs a superscript question mark which, when clicked, pops up a
     section in the wiki pages --%>

<c:set var="url" value="${WEB_PROPERTIES['project.helpLocation']}/${pageName}.html"/>

<c:if test="${!empty anchor}">
  <c:set var="url" value="${url}#"/>
  <c:set var="url" value="${url}${anchor}"/>
</c:if>

&nbsp;&nbsp;

<a class="contactTeam" onclick="showContactForm();return false;" href="#">Contact</a> <span class="omgDivider">&nbsp;</span>

<c:choose>

  <%-- templates --%>
  <c:when test="${pageName == 'templates'}">
    <html:link
      styleClass="contextHelpSmall"
      href="${WEB_PROPERTIES['project.tourLocation']}/Template_Queries.html"
      onclick="javascript:window.open('${WEB_PROPERTIES['project.tourLocation']}/Template_Queries.html','_help','toolbar=0,scrollbars=1,location=1,statusbar=1,menubar=0,resizable=1,width=800,height=600');return false">Help</html:link>
  </c:when>

  <%-- lists --%>
  <c:when test="${pageName == 'bag'}">
    <html:link
      styleClass="contextHelpSmall"
      href="${WEB_PROPERTIES['project.tourLocation']}/Creating_a_List.html"
      onclick="javascript:window.open('${WEB_PROPERTIES['project.tourLocation']}/Creating_a_List.html','_help','toolbar=0,scrollbars=1,location=1,statusbar=1,menubar=0,resizable=1,width=800,height=600');return false">Help</html:link>
  </c:when>

  <%-- QueryBuilder --%>
  <c:when test="${pageName == 'customQuery'}">
    <html:link
      styleClass="contextHelpSmall"
      href="${WEB_PROPERTIES['project.tourLocation']}/Query_Builder.html"
      onclick="javascript:window.open('${WEB_PROPERTIES['project.tourLocation']}/Query_Builder.html','_help','toolbar=0,scrollbars=1,location=1,statusbar=1,menubar=0,resizable=1,width=800,height=600');return false">Help</html:link>
  </c:when>

  <%-- MyMine --%>
  <c:when test="${pageName == 'mymine'}">
    <html:link
      styleClass="contextHelpSmall"
      href="${WEB_PROPERTIES['project.tourLocation']}/MyMine.html"
      onclick="javascript:window.open('${WEB_PROPERTIES['project.tourLocation']}/MyMine.html','_help','toolbar=0,scrollbars=1,location=1,statusbar=1,menubar=0,resizable=1,width=800,height=600');return false">Help</html:link>
  </c:when>

  <%-- home et al. --%>
  <c:otherwise>
    <html:link
      styleClass="contextHelpSmall"
      href="${WEB_PROPERTIES['project.tourLocation']}/start.html"
      onclick="javascript:window.open('${WEB_PROPERTIES['project.tourLocation']}/start.html','_help','toolbar=0,scrollbars=1,location=1,statusbar=1,menubar=0,resizable=1,width=800,height=600');return false">Help</html:link>
  </c:otherwise>

</c:choose>