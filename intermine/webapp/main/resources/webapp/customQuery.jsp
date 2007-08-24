<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- customQuery.jsp -->
<html:xhtml/>

<script type="text/javascript" src="js/niftycube.js"></script>
<script type="text/javascript" src="js/mymine.js"></script>

<div class="body">
    <div id="leftCol">  
      <div id="pageDesc" class="pageDesc"><p><fmt:message key="customQuery.intro"/></p></div>
	   <div class="actionArea">
	     <h2>Actions:</h2>
	     	  <html:link action="/tree">
	            Browse data model
	            <img border="0" class="arrow" src="images/right-arrow.gif" alt="->"/>
	          </html:link>
	          <br/>
	          <html:link action="/importQueries?query_builder=yes">
	            <fmt:message key="begin.import.query"/>
	            <img border="0" class="arrow" src="images/right-arrow.gif" alt="->"/>
	          </html:link>
	          <br/>
	          <%-- TODO this should be done elsewhere --%>	
	        <c:choose>  
            <c:when test="${empty PROFILE.username}">
            	<c:set var="linky" value="login.do?returnto=%2Fmymine.do%3Fpage%3Dsaved"/>
            	<html:link action="${linky}">
	            Login to view saved queries
	            <img border="0" class="arrow" src="images/right-arrow.gif" alt="->"/>
	          </html:link>
            </c:when>
            <c:otherwise>
            	<c:set var="linky" value="/mymine.do?page=saved"/>
            	<html:link action="${linky}">
	            View saved queries
	            <img border="0" class="arrow" src="images/right-arrow.gif" alt="->"/>
	          </html:link>
            </c:otherwise>
	        </c:choose>
	          

	          
          </div>
   </div>

   <div id="rightCol">
	<%-- class chooser --%>	
	<im:roundbox titleKey="customQuery.classChooser" stylename="welcome">
	  <tiles:insert name="classChooser.tile"/>
	</im:roundbox>
   </div>

	<div id="clearLine"> 
	 	    <%-- query history --%> 
	 	    <im:roundbox title="Query History" stylename="welcome"> 
	 	     <tiles:insert name="historyQueryView.jsp"> 
	 	        <tiles:put name="type" value="history"/> 
	 	      </tiles:insert> 	 	    
	 	     </im:roundbox> 
 	     


	</div> 


</div>

<script type="text/javascript">
	Nifty("div#pageDesc","big");
</script>
<!-- /customQuery.jsp -->