<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<html:xhtml/>

<html>
<head>
<title><fmt:message key="waiting.title"/></title>
    <meta http-equiv="refresh" content="${POLL_REFRESH_SECONDS};url=<html:rewrite action="${POLL_ACTION_NAME}?qid=${param.qid}"/>">
    <link rel="stylesheet" type="text/css" 
          href="${WEB_PROPERTIES["project.sitePrefix"]}/style/base.css"/>
    <link rel="stylesheet" type="text/css" 
          href="${WEB_PROPERTIES["project.sitePrefix"]}/style/branding.css"/>
    <link rel="stylesheet" type="text/css" href="webapp.css"/>
</head>
<body>
  <tiles:insert page="/header.jsp"/>
  <div class="waitmsg">
    <fmt:message key="waiting.message"/>${dots}
  </div>
</body>
