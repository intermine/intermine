<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- customQuery.jsp -->
<html:xhtml/>

<div style="float:left;width:45%;">
	<%-- class chooser --%>	
	<im:roundbox title="Select a Data Type" stylename="queries">	
		<jsp:include page="queryClassSelect.jsp"/>
	</im:roundbox>			
</div>

<div style="margin-left:50%;width:45%;">
	<%-- model browser --%>	
	<im:roundbox title="Browse Model" stylename="queries">	
		<tiles:insert name="tree.tile"/>		
	</im:roundbox>	
</div>

	        <div class="body">
	          <html:link action="/importQueries?query_builder=yes">
	            <fmt:message key="begin.import.query"/>
	            <img border="0" class="arrow" src="images/right-arrow.gif" alt="->"/>
	          </html:link><br/>
	        </div> 

<!-- /customQuery.jsp -->
