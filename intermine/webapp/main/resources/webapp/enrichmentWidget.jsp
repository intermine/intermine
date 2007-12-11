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
<tiles:importAttribute/>
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

<c:out value='${enrichmentWidgetForm.description}'/>  Smaller p-values show greater enrichment. Method: Hypergeometric test.
<br/><br/>
Reference population: <c:out value='${referencePopulation}'/>.
<br/><br/>


	
<table>	


<tr>
	<td valign="top" align="center">

	<html:form action="/enrichmentWidget" method="get">
	<table>
	<tr>
		<td>Error Correction</td>
		<td><html:select property="errorCorrection">
				<html:option value="Bonferroni">Bonferroni</html:option>
			</html:select>
		</td>
	</tr>
	<c:if test="${!empty enrichmentWidgetForm.filters}">
      <tr>
      	<td>${enrichmentWidgetForm.filterLabel}</td>
      	<td>
      	
    <c:set value="${enrichmentWidgetForm.filters}" var="filters" />
	<c:set var="list" value='${fn:split(filters, ",")}' />
      	
      		<html:select property="filter">
     		 <c:forEach items="${list}" var="name">
				<html:option value="${name}">${name}</html:option>
    		 </c:forEach>    		 
      		</html:select>	
			&nbsp;
	    </td>
	   </tr>	   
	   </c:if>	   
	   <tr>
	   	<td></td>
	   	<td><html:submit property="filterSubmit" value="Update results" /></td>
	   	</tr>
	</table>	
   		<html:hidden property="bagName"/>
      		    <html:hidden property="filters"/>
      		    <html:hidden property="controller"/>
      		   	<html:hidden property="title" />
      		    <html:hidden property="link" />
      		  	<html:hidden property="max" />
      		  	<html:hidden property="description" />
      		  	<html:hidden property="filterLabel"/>
      		  	<html:hidden property="label"/>
	
	</html:form>	
	
	</td>
</tr>


<tr>
	<td>
		<c:choose>
		<c:when test="${!empty pvalues}">
			<table cellpadding="5" border="0" cellspacing="0" class="results">
		  	<tr>	
  				<th>${enrichmentWidgetForm.label}</th>
	  			<th>p-value</th>
  				<th>&nbsp;</th>
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
  		   				<html:link action="/widgetAction?key=${results.key}&bagName=${enrichmentWidgetForm.bagName}&link=${enrichmentWidgetForm.link}" target="_top">
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