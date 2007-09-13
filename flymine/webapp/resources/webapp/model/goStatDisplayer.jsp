<%
String path = request.getContextPath();
String basePath = "http://"+request.getServerName()+":"+request.getServerPort()+path+"/layout.jsp";
%>


<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<!-- goStatDisplayer.jsp -->

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-1-transitional.dtd">

<html xmlns="http://www.w3.org/1999/xhtml" lang="en" xml:lang="en">

  <head>

    <base href="<%=basePath%>" />
    <link rel="stylesheet" type="text/css" href="model/css/gostat.css"/>
    <title>FlyMine: List Analysis Page</title>
  </head>

  
  <body> 

<html:xhtml/>


<c:if test="${ontology == 'biological_process'}">
	<c:set var="bioSELECTED">selected</c:set>
</c:if>	
<c:if test="${ontology == 'cellular_component'}">
	<c:set var="cellSELECTED">selected</c:set>
</c:if>	
<c:if test="${ontology == 'molecular_function'}">
	<c:set var="moleSELECTED">selected</c:set>
</c:if>	
<div class="body">

<center><h2>Gene Ontology Enrichment</h2></center>

GO terms that are enriched for genes in this list compared to the reference population.  Smaller p-values show greater enrichment. Method: Hypergeometric test with Bonferroni error correction (using a significance value of 0.05).
<br><br>
Reference population: <c:out value='${goStatOrganisms}'/>.
<br><br>


	
<table>	
<tr>
	<td valign="top" align="center">
	
	<!-- initGoStatDisplayer -->
	
	<form action="goStatDisplayer.do" method="get" name="goStatForm">
	<table>
      <tr>
      	<td>Ontology</td>
      	<td>
      		<select name="ontology">
           		<option value="biological_process" ${bioSELECTED}>Biological process</option>
           		<option value="cellular_component" ${cellSELECTED}>Cellular component</option>
           		<option value="molecular_function" ${moleSELECTED}>Molecular function</option>
      		</select>
			&nbsp;
      		<input type="hidden" name="bagName" value="${bagName}"/>
			<input type="submit" name="filterSubmit" value="Update results">
	    </td>
	   </tr>
	</table>
	</form>	
	</td>
</tr>
<tr>
	<td>
		<c:choose>
		<c:when test="${!empty goStatPvalues}">
			<table cellpadding="5" border="0" cellspacing="0" class="results">
		  	<tr>	
  				<th>GO Term</td>
	  			<th>p-value</td>
  				<th>&nbsp;</td>
			</tr>
	  		<c:forEach items="${goStatPvalues}" var="results">
    			<tr>  	
  					<td align="left"><c:out value='${goStatGoTermToId[results.key]}'/> [<c:out value='${results.key}'/>]</td>
  					
  					
  					
  					<td align="left">
  						<c:choose>
  						<c:when test="${results.value < 0.0000001}">
  							<fmt:formatNumber value="${results.value}" pattern="0.#######E0" minFractionDigits="7" maxFractionDigits="7" />
						</c:when>		
						<c:otherwise>
	  						<fmt:formatNumber value="${results.value}" minFractionDigits="7" maxFractionDigits="7" />
				        </c:otherwise>        
    				    </c:choose>
  					</td>
  					<td align="left" nowrap>
  		   				<html:link action="/goStatAction?key=${results.key}&bag=${bagName}" target="_top">
  		   					[<c:out value='${goStatGeneTotals[results.key]}'/> genes]
       					</html:link>  	
	       			</td>
				</tr>
			</c:forEach>
			</table>
		</c:when>		
		<c:otherwise>
	        No results found.
        </c:otherwise>        
        </c:choose>

	</td>
</tr>
</table>
            
 </div>
 </body>
 </html>
 
<!-- /goStatDisplayer.jsp -->