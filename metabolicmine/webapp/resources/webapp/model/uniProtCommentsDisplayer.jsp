<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<html:xhtml/>
<c:if test="${not empty response.value}">
	<div class="geneInformation">
		<h3 class="uniprot">Curated comments from UniProt</h3>
		<br /><br />
		<table cellspacing="0">
		
			<c:forEach var="type" items="${response}">
				<c:choose>
					<%-- displayer for gene page --%>
					<c:when test="${type.key == 'gene'}">
						<tr><th class="type">Type</th><th class="comment">Comment</th><th class="proteins">Proteins</th></tr>
						
						<%-- traverse the comments --%>
						<c:forEach var="comment" items="${type.value}" varStatus="status">
							<tr class="${status.count mod 2 == 0 ? 'odd' : 'even'}">
								<%-- comment type and proteins objects --%>
								<c:forEach var="bag" items="${comment.value}">
									<c:choose>
										<c:when test="${bag.key == 'type'}">
											<!-- comment 'type' -->
											<td class="type">
												${bag.value}
											</td>
											<td class="text">${comment.key}</td>
										</c:when>
										<c:when test="${bag.key == 'proteins'}">
											<!-- comment 'proteins' HashMap -->
											<td>
												<c:forEach var="protein" items="${bag.value}" varStatus="looptyLoop">
													<!-- protein: primaryIdentifier => id -->
													<html:link action="/objectDetails?id=${protein.value}&amp;trail=|${protein.value}">
														${protein.key}
													</html:link>
													<!-- ${!looptyLoop.last ? ', ' : ''} -->
												</c:forEach>
											</td>
										</c:when>
										<c:otherwise>
											<!-- big fat fail -->
										</c:otherwise>
									</c:choose>
								</c:forEach>
								
							</tr>
						</c:forEach>
						
					</c:when>
					
					<%-- displayer for protein page --%>
					<c:when test="${type.key == 'protein'}">
						<tr><th class="type">Type</th><th class="comment">Comment</th></tr>
						<c:forEach var="comment" items="${type.value}" varStatus="status">
							<tr class="${status.count mod 2 == 0 ? 'odd' : 'even'}">
								<td class="type">${comment.value}</td>
								<td>${comment.key}</td>
							</tr>
						</c:forEach>						
					</c:when>
					
					<c:otherwise></c:otherwise>
					
				</c:choose>
			</c:forEach>
		</table>
	</div>
</c:if>