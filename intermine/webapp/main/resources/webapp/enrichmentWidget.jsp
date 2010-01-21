<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<!-- enrichmentWidget.jsp -->
<html:xhtml/>


<link rel="stylesheet" type="text/css" href="css/enrichmentWidget.css"/>
<link rel="stylesheet" href="css/toolbar.css" type="text/css" media="screen" title="Toolbar Style" charset="utf-8">
<script type="text/javascript" src="js/toolbar.js"></script>

<div class="body">

<center><h2>${ewf.title}</h2></center>

<c:out value='${ewf.descr}'/>  Smaller p-values show greater enrichment.  Method: Hypergeometric test  
<br/><br/>
Reference population: <c:out value='${referencePopulation}'/>.
<br/>
<table>	

<tr>
	<td valign="top" align="left">

	<html:form action="/enrichmentWidget">
	<table>
	<tr>
		<td>Multiple Hypothesis Test Correction</td>
		<td><html:select property="errorCorrection">
				<html:option value="Benjamini and Hochberg">Benjamini and Hochberg</html:option>
				<html:option value="Bonferroni">Bonferroni</html:option>
				<html:option value="None">None</html:option>
			</html:select>
		</td>
	</tr>
	<c:if test="${!empty ewf.filters}">
      <tr>
      	<td>${ewf.filterLabel}</td>
      	<td>
      	
    	<c:set value="${ewf.filters}" var="filters" />
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
	   	<td>Maximum value to display</td>
		<td><html:select property="max">
				<html:option value="0.01">0.01</html:option>
				<html:option value="0.05">0.05</html:option>
				<html:option value="0.10">0.10</html:option>
				<html:option value="0.50">0.50</html:option>
				<html:option value="1.00">1.00</html:option>
			</html:select>
		</td>
	   	
	   	</tr>
	   <tr>
	   	<td></td>
	   	<td>	   	
	   	 <html:submit property="reloadWidget">
			Update results
        </html:submit>
	   	
	   	</td>
	   	</tr>
	</table>	
   		<html:hidden property="bagName" value="${ewf.bagName}"/>
   		<html:hidden property="bagType" value="${ewf.bagType}"/>
        <html:hidden property="filters" value="${ewf.filters}" />
        <html:hidden property="ldr" value="${ewf.ldr}"/>
        <html:hidden property="title" value="${ewf.title}" />
        <html:hidden property="link"  value="${ewf.link}" />      		  
        <html:hidden property="descr" value="${ewf.descr}" />
        <html:hidden property="filterLabel" value="${ewf.filterLabel}"/>
        <html:hidden property="label" value="${ewf.label}"/>	
    </html:form>
	</td>
</tr>


<tr>
	<td valign="top" align="left">
	
	Select values below and click on the 'Display' button to view the records in a results table.
	
<html:form action="/widgetAction" target="_top">
<div id="tool_bar_div">
<ul id="button_bar" >
	<li id="tool_bar_li_display" class="tb_button"><img style="cursor: pointer;" src="images/icons/null.gif" width="62" height="25" alt="Display" border="0" id="tool_bar_button_display" class="tool_bar_button"></li>
	<!-- <li id="tool_bar_li_export"><img style="cursor: pointer;" src="images/icons/null.gif" width="64" height="25" alt="Export" border="0" id="tool_bar_button_export" class="tool_bar_button"></li> -->
</ul>
</div>

<%-- display --%>
<div id="tool_bar_item_display" style="display:none;" class="tool_bar_item">
    <a href="javascript:document.widgetForm.submit();">Display checked items in results table</a>
    <hr>
  <a href="javascript:hideMenu('tool_bar_item_export')" >Cancel</a>
</div>
<%-- Export --%>
<div id="tool_bar_item_export" style="display:none;" class="tool_bar_item">
    <c:set var="tableName" value="${param.table}" scope="request"/>
    <c:set var="tableType" value="results" scope="request"/>
    <c:set var="pagedTable" value="${resultsTable}" scope="request"/>
    <tiles:get name="export.tile"/>
    <hr>
  <a href="javascript:hideMenu('tool_bar_item_export')" >Cancel</a>
</div>


</td>
</tr>

<tr>
	<td>
		
		<c:choose>
		<c:when test="${!empty pvalues}">
			<table cellpadding="5" border="0" cellspacing="0" class="results">
		  	<tr>	
  				<th>&nbsp;</th>
  				<th>${ewf.label}</th>
	  			<th>p-value</th>
  				<th>&nbsp;</th>
			</tr>
	  		<c:forEach items="${pvalues}" var="results">
    			<tr>
    				<td>    					
                          <html:multibox property="selected"
                                         styleId="selected_${results.key}">
                            <c:out value="${results.key}"/>
                          </html:multibox> 		
    				</td>
  					<td align="left">
  							<c:choose>
							<c:when test="${!empty ewf.externalLink}">
								<a href="${ewf.externalLink}${results.key}${append}" class="extlink" target="_new"><c:out value='${labelToId[results.key]}'/></a>
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
  		   				<html:link action="/widgetAction?key=${results.key}&bagName=${ewf.bagName}&link=${ewf.link}" target="_top">
  		   					[<c:out value='${totals[results.key]}'/>  ${ewf.bagType}s]
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
        <html:hidden property="bagType" value="${ewf.bagType}" />
        <html:hidden property="bagName" value="${ewf.bagName}" />
        <html:hidden property="link" value="${ewf.link}" />
	</html:form>	

	</td>
</tr>
</table>
            
 </div>
<!-- /enrichmentWidget.jsp -->
