<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<html:xhtml/>
<div class="geneInformation">
	<c:forEach var="protein" items="${object.proteins}">
		<h4><c:out value="${protein.primaryIdentifier}"/></h4>
		<table cellspacing="0">
			<c:forEach var="comment" items="${protein.comments}">
				<tr><td class="type">${comment.type}</td><td class="text">${comment.text}</td></tr>
			</c:forEach>
		</table>
	</c:forEach>
</div>