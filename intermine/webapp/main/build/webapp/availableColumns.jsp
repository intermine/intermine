<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- availableColumns.jsp -->
<%-- Tiles that prints out drop down box with with available columns, 
that can be added to results table, which id is saved in table parameter --%>

<tiles:importAttribute name="table" ignore="false" />

<select name="columnToAdd" id="columnToAdd">
    <c:choose>
	    <c:when test="${fn:length(availableColumns) == 0}">
	         <option value="" />No available columns 
	    </c:when>
	    <c:otherwise>
	        <c:forEach var="column" items="${availableColumns}">
                <option value="${column.key}">
                    <im:displaypath path="${column.key}"/>
                </option>
	        </c:forEach>
	    </c:otherwise>
    </c:choose>		
</select>	

<!-- /availableColumns.jsp -->
