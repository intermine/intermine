<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- customQuery.jsp -->
<html:xhtml/>

<div class="body">

<table border="0">
<tr>
<td valign="top" width="50%">
    
      <div id="pageDesc" class="pageDesc"><fmt:message key="begin.querybuilder"/></div>

</td>
<td valign="top" width="50%">


	<div style=" width:75%">
	     <h2>Actions:</h2>
	          <html:link action="/importQueries?query_builder=yes">
	            <fmt:message key="begin.import.query"/>
	            <img border="0" class="arrow" src="images/right-arrow.gif" alt="->"/>
	          </html:link>
            <BR/><BR/>         
       </div>
           
</td>
</tr>
</table>


	<%-- class chooser --%>	
	<div class="pageDesc" id="pageDesc" style="width:70%">		
	  <h2><fmt:message key="customQuery.classChooser"/></h2>
	  <tiles:insert name="classChooser.tile"/>
	</div>



<!-- /customQuery.jsp -->