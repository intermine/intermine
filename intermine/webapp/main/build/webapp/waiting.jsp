<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<html:xhtml/>

<html>
  <head>
    <title><fmt:message key="waiting.title"/></title>
    <link rel="stylesheet" type="text/css" href="css/webapp.css"/>
    <link rel="stylesheet" type="text/css" href="css/waiting.css"/>
    <c:set var="theme" value="${WEB_PROPERTIES['theme']}"/>
    <link rel="stylesheet" type="text/css" href="<html:rewrite page='/themes/${theme}/theme.css'/>"/>
    <noscript>
      <meta http-equiv="Refresh" content="${POLL_REFRESH_SECONDS}; URL=<html:rewrite action="${POLL_ACTION_NAME}?qid=${param.qid}"/>">
    </noscript>
  </head>
  <body>
<div width="100%" align="center"/> 
<div style="margin-top:200px">
    <div class="waitmsg">
      <fmt:message key="waiting.message"/>
      <p>
        <img border="0" src="model/images/progress${imgnum}.gif" title="Please wait, your query is running..."/>
      </p>
    </div>
    </div>
    <script language="JavaScript">
    function timedredirect()
    {
      window.location.replace("<html:rewrite action="${POLL_ACTION_NAME}?qid=${param.qid}&trail=${param.trail}&queryBuilder=${queryBuilder}"/>");
    }
    setTimeout("timedredirect()", ${POLL_REFRESH_SECONDS}*1000 );
    window.status = '';
    </script>
  </body>
</html>
