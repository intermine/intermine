<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<!-- addColumn.jsp -->
<%-- Tile that prints out form with drop down box with available columns, that can be added to results table, 
which id is saved in table parameter --%>

<tiles:importAttribute name="table" ignore="false" />
<tiles:importAttribute name="trail" ignore="false" />

<html:form action="/addColumn">
    Add column to results page <br />
    <tiles:insert name="availableColumns.tile">
        <tiles:put name="table" value="${table}" />
    </tiles:insert>    
    
    <input type="hidden" name="table" value="${table}" />
    <input type="hidden" name="trail" value="${trail}" />
    &nbsp;
    <html:submit value="Add"></html:submit>
</html:form>
<!-- /addColumns.jsp -->
