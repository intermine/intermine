<%@ tag body-content="empty" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ attribute name="section" required="true" %>

<%-- outputs a superscript question mark which, when clicked, pops up a
     section in the manual --%>

<script type="text/javascript" src="js/manualLink.js"/>

<c:set var="url" value="${WEB_PROPERTIES['project.helpLocation']}/${section}"/>

<sup><html:link href="${url}" onclick="javascript:window.open('${url}','_manual','toolbar=0,scrollbars=1,location=1,statusbar=1,menubar=0,resizable=1,width=800,height=600');return false">?</html:link></sup>
