<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-1-transitional.dtd">
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<%-- Page displaying preview of html output of webservice and its inclusion in the example page. If you modify
this page, be sure that it is consistent with serviceLink.jsp page - width of frame ... So the preview corresponds  to 
the link generated for user. --%>
<!-- linkPreview.jsp -->
<%@page import="java.net.URLDecoder"%>
<html>

<head>

<style type="text/css">
.content {
	background-color:#F4F4F4;
	border: 1px solid #7E7E7E;
}
</style>

</head>

<body title="Example of page with results">

<c:set var="link" value="${fn:replace(param.link, 'qwertyui', '&')}"></c:set> 

<h2 style="margin-bottom: 0px; font-family:Verdana,arial,Helvetica,sans-serif;">Your web page</h2>
    <table style="width: 700px; height: 110px; margin-top: 20px; margin-bottom: 20px;" class="content">
        <tr><td></td></tr>
        <tr><td align="center">Content of your  web page.</td></tr>
        <tr><td></td></tr>
    </table>

<div style="text-align: left;">
    Results from <c:out value="${WEB_PROPERTIES['project.title']}"/> included dynamically in your page:
    <iframe width="700" height="500" frameborder="1" scrolling="yes" marginheight="0" marginwidth="0" src="${link}&format=html"></iframe>
</div>           

</body>
</html>
<!-- /linkPreview.jsp -->

