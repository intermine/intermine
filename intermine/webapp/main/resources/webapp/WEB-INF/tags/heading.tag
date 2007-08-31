<%@ tag body-content="scriptless"  %>
<%@ attribute name="id" required="false" %>
<%@ attribute name="topLeftTile" required="false" %>
<%@ attribute name="initial" required="false" type="java.lang.Boolean" %>

<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<c:if test="${initial != null && COLLAPSED[id] == null}">
  <c:set target="${COLLAPSED}" property="${id}" value="${initial}"/>
</c:if>

<div class="listHeading">
  <c:if test="${!empty id}">
    <a href="javascript:toggleHidden('${id}');">
  	<c:choose>
      <c:when test="${COLLAPSED[id]}">
        <img border="0" src="images/disclosed.gif" alt="-" id="${id}Toggle" />
      </c:when>
      <c:otherwise>
        <img border="0" src="images/undisclosed.gif" alt="-" id="${id}Toggle" />
      </c:otherwise>
    </c:choose>
    </a>
  </c:if>  
  <jsp:doBody/>
</div>