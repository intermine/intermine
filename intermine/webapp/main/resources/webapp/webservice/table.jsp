<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil" %>

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
	font-size: 60%;
}

.title {
	padding:5px 10px 5px 15px;
	font-size: 70%;
	font-family:Verdana,arial,Helvetica,sans-serif;
	color:#444444;
	font-weight: bold;
	padding: 3px;
}

.mainBar {
    font-size: 14px;
    margin-left: 5px;
}

</style>

<title>Result table</title>
</head>

<body>

<div>
    <div class="title">${title}</div>
    <span class="description">${description}</span>
    <div style="font-size:14px;">
	     <span style="white-space:nowrap;">
	         &nbsp;<a href="\" onclick="javascript:window.open(window.location.href, '', 'fullscreen=yes, scrollbars=auto');return false;">Open in new window</a>"
	     </span>"
	     &nbsp;&nbsp;&nbsp;
	     Source:<a href="\" onclick="javascript:window.open('http://www.flymine.org');return false;" style="margin-left:10px;">Flymine</a>
	     &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
	     <imutil:navigationBar baseLink="${baseLink}" pageSize="${pageSize}" currentPage="${currentPage}" />
    </div>
    <imutil:table rows="${requestScope.rows}" columnNames="${requestScope.columnNames}" treatColNames="true"/>
</div>

</body>
</html>