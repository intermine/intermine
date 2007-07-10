<%@ tag body-content="scriptless" %>
<%@ attribute name="text" required="false" %>
<%@ attribute name="skipBuilder" required="true" type="java.lang.Boolean" %>
<%@ attribute name="showArrow" required="false" type="java.lang.Boolean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<jsp:doBody var="query"/>

<jsp:useBean id="linkParams" scope="page" class="java.util.TreeMap">
  <c:set target="${linkParams}" property="method" value="xml" />
  <c:set target="${linkParams}" property="query" value="${query}" />
  <c:set target="${linkParams}" property="skipBuilder" value="${skipBuilder}" />
  <c:set target="${linkParams}" property="trail" value="|query" />
</jsp:useBean>

<html:link action="/loadQuery" name="linkParams">
  <span>
    ${text}
    <c:if test="${showArrow}">
      <img border="0" class="arrow" src="images/right-arrow.gif" alt="-&gt;"/>
    </c:if>
  </span>
</html:link>
