<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<html:xhtml/>
<div class="geneInformation">
	<table cellspacing="0">
		<c:forEach var="comment" items="${object.comments}">
			<tr><td class="type">${comment.type}</td><td class="text">${comment.text}</td></tr>
		</c:forEach>
	</table>
</div>