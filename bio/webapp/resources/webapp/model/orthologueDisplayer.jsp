<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- orthologueDisplayer.jsp -->

<html:xhtml/>

<div class="body">
<table class="results">
<tr>		      	
  	<c:forEach items="${orthos}" var="org">
  	<td align="center"> 
		<c:choose> 
    	<c:when test="${org.value[0] != 0}">
	       
			<c:choose> 
    			<c:when test="${org.value[0] != 1}">
	       		<html:link action="/orthologueAction?bagName=${bag.name}&query=${org.value[1]}">		        	         
      			[${org.key}]<br><br><c:out value="${org.value[0]}" />
       			</html:link>       		
				</c:when>
	    		<c:otherwise>
   				<html:link action="/orthologueAction?id=${org.value[2]}&query=${org.value[1]}">		        	         
      			[${org.key}]<br><br><c:out value="${org.value[0]}" />
       			</html:link>  			
			</c:otherwise>
			</c:choose>

       		
		</c:when>
	    <c:otherwise>
			<c:out value="${org.key}" /><br><br>0
		</c:otherwise>
		</c:choose>
	</td>
	</c:forEach>
</tr>
</table>
 </div>

<!-- orthologueDisplayer.jsp -->