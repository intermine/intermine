<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">

<style type="text/css">

.description {
	font-style: italic;
	color: #333333;
	padding: 3px;
	font-size: 60%;
}

.title {
	padding:5px 10px 5px 15px;
	font-size: 70%;
	font-family:Verdana,arial,Helvetica,sans-serif;
	color:#444444;
	/*background-color: #f0f0f0;*/
	font-weight: bold;
	padding: 3px;
}

table tr th {
	font-size: 90%;
	white-space: nowrap;
	background-color:#EEEEEE;
	vertical-align:middle;
	padding:0.25em 0.25em;
	font-weight: bold;
}

table tr td {
	font-size: 80%;
	vertical-align:middle;
}

</style>

<title>Result table</title>
</head>

<body>

<div>
<!--	<h3 style="color:#CE0808;">Result table</h3>-->
	<c:if test="${requestScope.htmlTable != null}">
		${requestScope.htmlTable.HTML}
	</c:if>
	</div>
</div>

</body>
</html>