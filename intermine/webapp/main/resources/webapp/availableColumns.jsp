<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<!-- availableColumns.jsp -->
<%-- Tiles that prints out drop down box with with available columns, 
that can be added to results table, which id is saved in table parameter --%>

<tiles:importAttribute name="table" ignore="false" />

<select name="columnToAdd" id="columnToAdd">
<c:forEach var="column" items="${availableColumns}">
    <option value="${column.key}" />${column.value}
</c:forEach>
</select>

<!-- /availableColumns.jsp -->
