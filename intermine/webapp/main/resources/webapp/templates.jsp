<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>


<!-- templates.jsp -->
<html:xhtml/>

<div class="body">
<%--<div id="leftCol">
   <div id="pageDesc" class="pageDesc"><p><fmt:message key="templates.intro"/></p></div>
	 <div class="actionArea">
	     <h2>Actions:</h2>
  	        <c:choose>  
            <c:when test="${empty PROFILE.username}">
            	<c:set var="linky" value="login.do?returnto=%2Fmymine.do%3Fpage%3Dtemplates"/>
            	<html:link action="${linky}">
	            Login to manage your templates
	            <img border="0" class="arrow" src="images/right-arrow.gif" alt="->"/>
	          </html:link>
            </c:when>
            <c:otherwise>
            	<c:set var="linky" value="/mymine.do?page=templates"/>
            	<html:link action="${linky}">
	            Manage my templates
	            <img border="0" class="arrow" src="images/right-arrow.gif" alt="->"/>
	          </html:link>
            </c:otherwise>
	        </c:choose>
           
         <BR/>
           <html:link action="/import" titleKey="begin.importTemplatesDesc">
             <fmt:message key="begin.importTemplates"/>
             <img border="0" class="arrow" src="images/right-arrow.gif" alt="->"/>
           </html:link>
            <BR/><BR/>         
    </div>
    
    
    
</div>
<div id="rightCol">--%>
      <html:form action="/modifyTemplate">
        <tiles:insert name="wsTemplateTable.tile">
          <tiles:put name="wsListId" value="all_templates"/>
          <tiles:put name="scope" value="all"/>
          <tiles:put name="makeCheckBoxes" value="true"/>
          <tiles:put name="showNames" value="false"/>
          <tiles:put name="showTitles" value="true"/>
          <tiles:put name="showDescriptions" value="true"/>
          <tiles:put name="showSearchBox" value="true"/>
          <tiles:put name="initialFilterText" value="${initialFilterText}"/>
        </tiles:insert>
      </html:form>
</div>
<!-- </div> -->

<!-- /templates.jsp -->
