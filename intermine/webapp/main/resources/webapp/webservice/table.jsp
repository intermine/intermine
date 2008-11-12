<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil" %>
<script type="text/javascript" src="<html:rewrite page='/js/prototype.js'/>"></script>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<link>

<%-- JSP page rendering simple html output of results from web service --%>
<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/webapp.css"/>
<%-- Imported to be the style of table same as style of results table in webapp --%>
<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/resultstables.css"/>

<style type="text/css">

.description {
	font-style: italic;
	color: #333333;
	padding: 3px;
	width: 300px;
	font-size: 90%;
}

.title {
	padding:5px 10px 5px 15px;
/*	font-size: 70%;*/
	font-family:Verdana,arial,Helvetica,sans-serif;
	color:#444444;
	font-weight: bold;
	padding: 3px;
}

.mainBar {
    font-size: 14px;
    margin-left: 5px;
}

body {
    font-size:14px;
}


</style>

<script type="text/javascript">
    // will be done when page is loaded
	var url = window.location.href + '&tcount';
	url = url.replace('format=html', 'format=tab');
	
	new Ajax.Request(url, 
	{
	  method: 'get',
	  onSuccess: function(transport) {
	    var countEl = document.getElementById('resultCount');
	    countEl.innerHTML = transport.responseText;
	  }
	});
</script>

<title>Result table</title>
</head>

<body>

<div>
    <c:if test="${fn:contains(layout, 'title') || fn:contains(layout, 'all')}">
        <div class="title">${title}</div>
    </c:if>
    
    <c:if test="${fn:contains(layout, 'description') || fn:contains(layout, 'all')}">
        <span class="description">${description}</span>
    </c:if>
    
    <div>
        
        <c:if test="${fn:contains(layout, 'mineresults') || fn:contains(layout, 'all')}">
	         <span style="white-space:nowrap;">
	           <a href="" onclick="javascript:window.open('${mineLinkUrl}');return false;"><c:out value="${mineLinkText}" /></a>
	         </span>
         </c:if>
         
         <c:if test="${fn:contains(layout, 'paging') || fn:contains(layout, 'all')}">
		     <span style="margin-left:15px;">
		       <imutil:navigationBar baseLink="${baseLink}" pageSize="${pageSize}" currentPage="${currentPage}" nextEnabled="${pageSize == fn:length(rows)}"/>
		     </span>
	     </c:if>
	    
	     <c:if test="${fn:contains(layout, 'count') || fn:contains(layout, 'all')}">
		     <span style="margin-left:15px;">
		       Results:&nbsp;<span id="resultCount">in progress</span>
		     </span>
	     </c:if>
	     
	     <c:if test="${fn:contains(layout, 'newwindow') || fn:contains(layout, 'all')}">
	         <span style="white-space:nowrap; margin-left:15px;">
	           <a href="" onclick="javascript:window.open(window.location.href);return false;">Open in new window</a>
	         </span>
         </c:if>
    </div>
    <c:choose>
        <c:when test="${currentPage == 0 || fn:contains(layout, 'all')}">
            <c:set var="noResultsMsg" value="There are no results." />
        </c:when>
        <c:otherwise>
            <c:set var="noResultsMsg" value="There are no further results." />
        </c:otherwise>
    </c:choose>
    <imutil:table rows="${rows}" columnNames="${columnNames}" treatColNames="true" noResultsMessage="${noResultsMsg}"/>
    <c:if test="${fn:contains(layout, 'mine') || fn:contains(layout, 'all')}">
        <a href="" onclick="javascript:window.open('${WEB_PROPERTIES['project.sitePrefix']}');return false;">
            ${WEB_PROPERTIES['project.title']}
        </a> website.
    </c:if>
</div>

</body>
</html>