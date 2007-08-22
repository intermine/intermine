<%@ tag body-content="scriptless"  %>
<%@ attribute name="id" required="false" %>
<%@ attribute name="topLeftTile" required="false" %>
<%@ attribute name="initial" required="false" type="java.lang.Boolean" %>
<%@ attribute name="index" required="false" %>

<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<c:if test="${initial != null && COLLAPSED[index] == null}">
  <c:set target="${COLLAPSED}" property="${index}" value="${initial}"/>
</c:if>

<div class="listHeading">
  <c:if test="${!empty id}">
    <a href="javascript:toggleHidden('template${index}');">
  	<c:choose>
      <c:when test="${COLLAPSED[index]}">
        <img border="0" src="images/disclosed.gif" alt="-" id="template${index}$Toggle" />
      </c:when>
      <c:otherwise>
        <img border="0" src="images/undisclosed.gif" alt="-" id="template${index}Toggle" />
      </c:otherwise>
    </c:choose>
    </a>
  </c:if>  
  <jsp:doBody/>
</div>