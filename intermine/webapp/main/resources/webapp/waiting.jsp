<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<html:xhtml/>

<html>
  <head>
    <title><fmt:message key="waiting.title"/></title>
    <link rel="stylesheet" type="text/css" href="webapp.css"/>
    <link rel="stylesheet" type="text/css" href="model/model.css"/>
    <noscript>
      <meta http-equiv="Refresh" content="${POLL_REFRESH_SECONDS}; URL=<html:rewrite action="${POLL_ACTION_NAME}?qid=${param.qid}"/>">
    </noscript>
  </head>
  <body>
    <tiles:insert page="/header.jsp"/>
    <div class="waitmsg">
      <fmt:message key="waiting.message"/>
      <p>
        <img border="0" src="model/progress${imgnum}.gif" alt="progress"/>
      </p>
    </div>
    <script language="JavaScript">
  <!--
    function timedredirect()
    {
      window.location.replace("<html:rewrite action="${POLL_ACTION_NAME}?qid=${param.qid}&trail=${param.trail}"/>");
    }
    setTimeout("timedredirect()", ${POLL_REFRESH_SECONDS}*1000 );
    window.status = '';
  //-->
    </script>
  </body>
</html>
