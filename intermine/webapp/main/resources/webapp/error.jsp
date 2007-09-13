<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<tiles:importAttribute/>

<!-- error.jsp -->
<html:xhtml/>

<c:set var="msg" value="I found this error on ${pageContext.request.requestURL}.%0D%0A%0D%0A---- Error Found ----%0D%0A%0D%0A${stacktrace}"/>
<c:set var="subject" value="Error found on website"/>
<div id="generic_error">
  <div id="errorApology">
    <p>
      There has been an internal error while processing your request.  The
      problem has been logged and will be investigated.  You may also 
      <html:link href="mailto:info@flymine.org?body=${msg}&subject=${subject}">send us an email</html:link>
      describing how you encountered this error.
    </p>
    <p>
      The problem may be
      temporary in which case you might wish to
      <html:link href="javascript:history.back()">
        go back
      </html:link>
      and try your request again or you might want to go to the
      <html:link href="${WEB_PROPERTIES['project.sitePrefix']}/">
        home page.
      </html:link>
    </p>
  </div>
</div>
<c:if test="${!empty stacktrace}">
  <c:choose>
    <c:when test="${IS_SUPERUSER}">
      <style>
pre.stacktrace {
 background: #f7f7f7;
 border: 1px solid #d7d7d7;
 padding: .25em;
 overflow: auto;
}

div.error_body {
  background-color: white;
  border: solid 1px #bbb;
}
      </style>
      <div class="error_body">
        <div class="body"><b><fmt:message key="error.stacktrace"/></b></div>

        <div class="body">
          <pre class="stacktrace">
	    <c:out value="${stacktrace}"/>
          </pre>
        </div>
      </div>
    </c:when>
    <c:otherwise>
<!--
	    <c:out value="${stacktrace}"/>
-->
    </c:otherwise>
  </c:choose>
</c:if>
<!-- /error.jsp -->
