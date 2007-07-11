<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- customQuery.jsp -->
<html:xhtml/>

<div style="float:left;width:45%;">

	<%-- links --%>
	<im:roundbox title="Custom Queries" color="roundcorner">
	        <div class="body">
	          <html:link action="/mymine.do?page=bags">
	            <fmt:message key="begin.upload.identifiers"/>
	            <img border="0" class="arrow" src="images/right-arrow.gif" alt="->"/>
	          </html:link><br/>
	          <html:link action="/tree">
	            <fmt:message key="begin.browse.model"/>
	            <img border="0" class="arrow" src="images/right-arrow.gif" alt="->"/>
	          </html:link><br/>
	          <html:link action="/importQueries?query_builder=yes">
	            <fmt:message key="begin.import.query"/>
	            <img border="0" class="arrow" src="images/right-arrow.gif" alt="->"/>
	          </html:link><br/>
	        </div>   
	</im:roundbox>

	<%-- class chooser --%>	
	<im:roundbox title="Start a New Query" color="roundcorner">	
		<jsp:include page="queryClassSelect.jsp"/>
	</im:roundbox>	
	
</div>


<div style="margin-left:50%;width:45%;">

	<%-- saved queries --%>
	<im:roundbox title="Saved Queries" color="roundcorner" >
	  <tiles:insert name="historyQueryView.jsp">
        <tiles:put name="type" value="saved"/>
      </tiles:insert>
	</im:roundbox>
	
</div>
<!-- /customQuery.jsp -->
