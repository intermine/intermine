<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<html:xhtml/>
<c:if test="${not empty comments}">
	<div class="geneInformation">
		<h3 class="uniprot">Curated comments from UniProt</h3>
		<br /><br />
		
		<table cellspacing="0">
			<tr><th class="comment">Comment</th><th class="type">Type</th><th class="proteins">Proteins</th></tr>
			<c:forEach var="comment" items="${comments}" varStatus="status">
				<tr class="${status.count mod 2 == 0 ? 'odd' : 'even'}">
					<td class="text">${comment.key}</td>
					<c:forEach var="bag" items="${comment.value}">
						<td class="text">
							<c:choose>
								<c:when test="${bag.key == 'type'}">
									<!-- comment 'type' -->
									${bag.value}
								</c:when>
								<c:otherwise>
									<!-- assume 'proteins' -->
									<c:forEach var="protein" items="${bag.value}" varStatus="looptyLoop">
										${protein}
										${!looptyLoop.last ? ', ' : ''}
									</c:forEach>
								</c:otherwise>
							</c:choose>
						</td>
					</c:forEach>
				</tr>
			</c:forEach>
		</table>
	</div>
</c:if>