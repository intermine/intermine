<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- customQuery.jsp -->
<html:xhtml/>
<div class="body">
<div style="float:left;width:35%;">
	<%-- class chooser --%>
	<im:roundbox title="Select a Data Type" stylename="welcome">
		<tiles:insert name="classChooser.tile"/>
	</im:roundbox>

	<%--  actions --%>
      <div id="actionsLeft" class="body">
         <h2>Actions:</h2>
	          <html:link action="/importQueries?query_builder=yes">
	            <fmt:message key="begin.import.query"/>
	            <img border="0" class="arrow" src="images/right-arrow.gif" alt="->"/>
	          </html:link>
      </div>

</div>
<div style="margin-left:40%;width:60%;">

	<%-- saved queries --%>
	<im:roundbox title="Saved Queries" stylename="welcome">
	 <tiles:insert name="historyQueryView.jsp">
        <tiles:put name="type" value="saved"/>
      </tiles:insert>
	</im:roundbox>

	<%-- query history --%>
	<im:roundbox title="Query History" stylename="welcome">
	 <tiles:insert name="historyQueryView.jsp">
        <tiles:put name="type" value="history"/>
      </tiles:insert>
	</im:roundbox>
	</div>
</div>
<!-- /customQuery.jsp -->