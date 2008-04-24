<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<%-- Page displaying preview of html output of webservice and its inclusion in the example page. If you modify
this page, be sure that it is consistent with serviceLink.jsp page - width of frame ... So the preview corresponds  to 
the link generated for user. --%>
<!-- linkPreview.jsp -->
<?xml version="1.0" encoding="iso-8859-1"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-1-transitional.dtd">
<html>

<head>

</head>

<body title="Example of page with results">

<c:set var="link" value="${fn:replace(param.link, 'XXXXX', '&')}"></c:set>

<h1>Drosophila</h1>

<div>
		    
	<i>"Drosophila is a genus of small flies, belonging to the family Drosophilidae, whose members are 
	often called "fruit flies" or more appropriately vinegar flies, wine flies, pomace flies, grape 
	flies, and picked fruit-flies, a reference to the characteristic of many species to linger around 
	overripe or rotting fruit. ... Scientists who research Drosophila are often called Drosophilists."</i>
	
</div>

<div style="margin-top: 10px; margin-bottom: 10px;">
    <i style="font-size: smaller;">This is an example of page with your included results. Citation used from <a href="http://en.wikipedia.org/w/index.php?title=Drosophila&oldid=205673247">Wikipedia</a></i>
</div>

<div style="text-align: center;">
    <iframe width="700" height="500" frameborder="1" scrolling="yes" marginheight="0" marginwidth="0" src="${link}&format=html"></iframe>
</div>           

</body>
</html>
<!-- /linkPreview.jsp -->

