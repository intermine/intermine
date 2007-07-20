<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- customQuery.jsp -->
<html:xhtml/>

<div class="body">
<table padding="0px" margin="0px" width="100%">
  <tr>
    <td valign="top" width="30%">
      <div id="pageDesc" class="queryStyle"><p><fmt:message key="begin.querybuilder"/></p></div>
      <im:roundbox title="Actions" stylename="welcome">
	          <html:link action="/importQueries?query_builder=yes">
	            <fmt:message key="begin.import.query"/>
	            <img border="0" class="arrow" src="images/right-arrow.gif" alt="->"/>
	          </html:link>
      </im:roundbox>
	</td>
	<td valign="top" width="70%">
	<%-- class chooser --%>	
	<div class="webSearchable" id="classChooser">		
	  <h2><fmt:message key="customQuery.classChooser"/></h2>
	  <tiles:insert name="classChooser.tile"/>
	</div>

</td>
</tr><tr>
<td colspan=2 valign="top">


	<%-- query history --%>
	<im:roundbox title="Query History" stylename="welcome">
	 <tiles:insert name="historyQueryView.jsp">
        <tiles:put name="type" value="history"/>
      </tiles:insert>
	</im:roundbox>
	
	<%-- saved queries --%>
	<im:roundbox title="Saved Queries" stylename="welcome">
	 <tiles:insert name="historyQueryView.jsp">
        <tiles:put name="type" value="saved"/>
      </tiles:insert>
	</im:roundbox> 


    </td>
  </tr>
</table>
</div>

<script type="text/javascript">
	Nifty("div#classChooser","big");
	Nifty("div#pageDesc","big");
</script>
<!-- /customQuery.jsp -->