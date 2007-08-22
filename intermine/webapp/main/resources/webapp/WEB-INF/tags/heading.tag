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

<div class="templateListHeading">
  <c:if test="${!empty id}">
    <c:set var="uri" value="${requestScope['javax.servlet.forward.servlet_path']}?${pageContext.request.queryString}"/>
    <jsp:useBean id="linkParams" scope="page" class="java.util.TreeMap">
      <c:set target="${linkParams}" property="id" value="${id}" />
      <c:set target="${linkParams}" property="forward" value="${uri}" />
    </jsp:useBean>
    <a href="javascript:toggleHidden('template${index}');">
  	<c:choose>
      <c:when test="${COLLAPSED[index]}">
        <img border="0" src="images/minus.gif" alt="-" id="template${index}Toggle" height="11" width="11"/>
      </c:when>
      <c:otherwise>
        <img border="0" src="images/plus.gif" alt="-" id="template${index}Toggle" height="11" width="11"/>
      </c:otherwise>
    </c:choose>
    </a>
    </c:if>  
  	<jsp:doBody/>
</div>