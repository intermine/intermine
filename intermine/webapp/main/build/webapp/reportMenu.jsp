<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil"%>

<tiles:importAttribute name="summary" ignore="true" />

<%-- jQuery.scrollTo relies on inlinetemplate.js! --%>

<div class="wrap">
  <span>Quick Links:</span>
  <div class="menu-wrap">
    <div class="links">
      <html:link action="/report.do?id=${object.id}#summary"
      onclick="jQuery('a[name=summary]').scrollTo('slow', 'swing', 0);return false;"
      styleClass="${summary}"
      >Summary</html:link>

      <c:forEach items="${categories}" var="aspect">
        <c:set var="target" value="${fn:replace(fn:toLowerCase(aspect), ' ', '_')}"/>
        <html:link
        action="/report.do?id=${object.id}#${target}"
        onclick="jQuery('a[name=${target}]').scrollTo('slow', 'swing', -21);return false;"
        >${aspect}</html:link>
      </c:forEach>

      <c:if test="${fn:length(placementRefsAndCollections['im:aspect:Miscellaneous']) > 0 || fn:length(listOfUnplacedInlineLists) > 0}">
        <html:link action="/report.do?id=${object.id}#other"
        onclick="jQuery('a[name=other]').scrollTo('slow', 'swing', -21);return false;"
        >Other</html:link>
      </c:if>
    </div>
  </div>
  <div class="clear">&nbsp;</div>
</div>