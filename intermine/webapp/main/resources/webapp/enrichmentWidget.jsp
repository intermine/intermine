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

<!-- enrichmentWidget -->

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-1-transitional.dtd">

<html xmlns="http://www.w3.org/1999/xhtml" lang="en" xml:lang="en">

  <head>

    <base href="<%=basePath%>" />
    <link rel="stylesheet" type="text/css" href="css/enrichmentWidget.css"/>
    <title>List Analysis Page</title>
  </head>

  
  <body> 

<html:xhtml/>


<div class="body">

<center><h2>${title}</h2></center>

<c:out value='${description}'/>  Smaller p-values show greater enrichment. Method: Hypergeometric test with Bonferroni error correction (using a significance value of 0.05).
<br><br>
Reference population: <c:out value='${referencePopulation}'/>.
<br><br>


	
<table>	

<c:if test="${!empty filters}">
<tr>
	<td valign="top" align="center">

	<form action="enrichmentWidget.do" method="get" name="goStatForm">
	<table>
      <tr>
      	<td>${filterLabel}</td>
      	<td>
      		<select name="filter">
     		 <c:forEach items="${filters}" var="name">
				<option value="${name}" <c:if test="${filter == name}">SELECTED</c:if>>${name}</option>
    		 </c:forEach>
      		</select>	
			&nbsp;
      			<input type="hidden" name="bagName" value="${bagName}"/>
      		    <input type="hidden" name="filters" value="${filters}"/>
      		    <input type="hidden" name="controller" value="${controller}"/>
      		   	<input type="hidden" name="title" value="${title}"/>
      		   	<input type="hidden" name="link" value="${link}"/>
      		  	<input type="hidden" name="max" value="${max}"/>
      		  	<input type="hidden" name="description" value="${description}"/>
      		  	<input type="hidden" name="filterLabel" value="${filterLabel}"/>
			<input type="submit" name="filterSubmit" value="Update results">
	    </td>
	   </tr>
	</table>
	</form>	
	
	</td>
</tr>
</c:if>

<tr>
	<td>
		<c:choose>
		<c:when test="${!empty pvalues}">
			<table cellpadding="5" border="0" cellspacing="0" class="results">
		  	<tr>	
  				<th>${label}</td>
	  			<th>p-value</td>
  				<th>&nbsp;</td>
			</tr>
	  		<c:forEach items="${pvalues}" var="results">
    			<tr>  	    			
  					<td align="left">
  							<c:choose>
							<c:when test="${!empty externalLink}">
								<a href="${externalLink}${results.key}${append}" class="extlink" target="_new"><c:out value='${labelToId[results.key]}'/></a>
					 		</c:when>		
							<c:otherwise>	
								<c:out value='${labelToId[results.key]}'/>
					        </c:otherwise>        
					        </c:choose>
  						<c:if test="${labelToId[results.key] != results.key}">[<c:out value='${results.key}'/>]</c:if>
 					</td>  				
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
  		   				<html:link action="/widgetAction?key=${results.key}&bagName=${bagName}&link=${link}" target="_top">
  		   					[<c:out value='${totals[results.key]}'/>  ${bagType}s]
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
 
<!-- /enrichmentWidget -->