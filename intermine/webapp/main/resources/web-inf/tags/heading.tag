<%@ tag body-content="scriptless"  %>
<%@ attribute name="id" required="false" %>
<%@ attribute name="initial" required="false" type="java.lang.Boolean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<c:if test="${initial != null && COLLAPSED[id] == null}">
  <c:set target="${COLLAPSED}" property="${id}" value="${initial}"/>
</c:if>

<div class="heading">
  <c:if test="${!empty id}">
    <c:set var="uri" value="${requestScope['javax.servlet.forward.servlet_path']}?${pageContext.request.queryString}"/>
    <jsp:useBean id="linkParams" scope="page" class="java.util.TreeMap">
      <c:set target="${linkParams}" property="id" value="${id}" />
      <c:set target="${linkParams}" property="forward" value="${uri}" />
    </jsp:useBean>
    <html:link action="/collapseElement" name="linkParams">
  	<c:choose>
      <c:when test="${COLLAPSED[id]}">
        <img border="0" src="images/undisclosed.gif" alt="+"/>
      </c:when>
      <c:otherwise>
        <img border="0" src="images/disclosed.gif" alt="-"/>
      </c:otherwise>
    </c:choose>
    </html:link>
    <html:link action="/collapseElement" name="linkParams">
    <jsp:doBody/>
    </html:link>
  </c:if>
  <c:if test="${empty id}"><jsp:doBody/></c:if>
</div>

