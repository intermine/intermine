<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<!-- addColumn.jsp -->
<%-- Tile that prints out form with drop down box with available columns, that can be added to results table, 
which id is saved in table parameter --%>

<tiles:importAttribute name="table" ignore="false" />
<tiles:importAttribute name="trail" ignore="false" />

<html:form action="/addColumn">
    <input type="hidden" name="trail" value="${trail}" />
    <input type="hidden" name="table" value="${table}" />
    Add column to results page <br />
    <%-- Select box with non restricted size and button are in hidden table, so they are drawn at one line
    with needed size in both FF and IE--%>
    <table><tr>
	    <td>
	        <tiles:insert name="availableColumns.tile">
	            <tiles:put name="table" value="${table}" />
	        </tiles:insert>        
	    </td>
	    <td>
	        &nbsp;<html:submit value="Add" styleId="columnAddSubmit"></html:submit>
	    </td>
    </tr></table>
</html:form>
<!-- /addColumns.jsp -->

<script type="text/javascript">
	    if (document.getElementById('columnToAdd')[0].value == '') {
	        document.getElementById("columnAddSubmit").disabled = true;
	        document.getElementById("columnToAdd").disabled = true;
		}		
</script>
