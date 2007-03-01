<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<!-- goStatDisplayer.jsp -->



<html:xhtml/>

<div class="body">

<table>
<tr>
	<td>	
	
<table cellpadding="5" border="0" cellspacing="0" class="results">	
  	<tr>	
  		<th>GO Term</td>
  		<th>p-value</td>
  		<th>&nbsp;</td>
	</tr>
  	<c:forEach items="${pvalues}" var="results"  varStatus="status">
  	    <c:set var="object" value="${row[0]}" scope="request"/>
       		<c:set var="rowClass">
       		<c:choose>
          		<c:when test="${status.count % 2 == 1}">odd</c:when>
				<c:otherwise>even</c:otherwise>
			</c:choose>
		</c:set>

    <tr class="${rowClass}">  	
  	<tr>	
  		<td align="left"><c:out value="${results.key}" /></td>
  		<td align="left"><!-- <c:out value="${results.value}" />-->
  		<fmt:formatNumber value="${results.value}" minFractionDigits="7" maxFractionDigits="7" />
  		</td> 		
  		<td align="left">
  		   		<html:link action="/goStatAction?key=${results.key}&bag=${bag.name}">
  		   			[view genes ...]
       			</html:link>  	
       	</td>
	</tr>
	</c:forEach>
</table>
</td><td valign="top">
	<form action="bagDetails.do" method="get" name="goStatForm">
	<table>
      <tr>
      	<td>Ontology</td>
      	<td>
      		<select name="ontology">
           		<option value="biological_process">Biological process</option>
           		<option value="cellular_component">Cellular component</option>
           		<option value="molecular_function">Molecular function</option>
      		</select>
      	</td>
      </tr>
      <tr>
      	<td>&nbsp;</td>
      	<td>
      		<input type="hidden" name="bagName" value="${bag.name}"/>
            <input type="hidden" name="table" value="${param.table}"/>
            <input type="hidden" name="trail" value="${param.trail}"/>
			<input type="submit" name="filterSubmit" value="Update results">
	    </td>
	   </tr>
	</table>
	</form>
	
</td>
</tr>
</table>


 </div>
 
 
<!-- /goStatDisplayer.jsp -->