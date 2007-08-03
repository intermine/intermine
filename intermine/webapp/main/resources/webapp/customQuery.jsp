<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- customQuery.jsp -->
<html:xhtml/>

<div class="body">
    <div id="leftCol">  
      <div id="pageDesc" class="pageDesc"><p><fmt:message key="customQuery.intro"/></p></div>
	   <div class="actionArea">
	     <h2>Actions:</h2>
	          <html:link action="/importQueries?query_builder=yes">
	            <fmt:message key="begin.import.query"/>
	            <img border="0" class="arrow" src="images/right-arrow.gif" alt="->"/>
	          </html:link>
	          <br/>
	          <html:link action="/mymine.do?page=history">
	            View query history
	            <img border="0" class="arrow" src="images/right-arrow.gif" alt="->"/>
	          </html:link>
	          
          </div>
   </div>

   <div id="rightCol">
	<%-- class chooser --%>	
	<im:roundbox titleKey="customQuery.classChooser" stylename="welcome">
	  <tiles:insert name="classChooser.tile"/>
	</im:roundbox>
   </div>


</div>

<script type="text/javascript">
	Nifty("div#pageDesc","big");
</script>
<!-- /customQuery.jsp -->