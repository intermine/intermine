<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>


<!-- templates.jsp -->
<html:xhtml/>

<div class="body">
<div id="leftCol">
   <div id="pageDesc" class="pageDesc"><p><fmt:message key="begin.templates"/></p></div>
	 <div class="actionArea">
	     <h2>Actions:</h2>
           <html:link action="/summariseAllTemplates" titleKey="begin.summariseAllTemplatesDesc">
             <fmt:message key="begin.summariseAllTemplates"/>
           </html:link>
         <BR/>
           <html:link action="/import" titleKey="begin.importTemplatesDesc">
             <fmt:message key="begin.importTemplates"/>
           </html:link>
            <BR/><BR/>         
    </div>
</div>
<div id="rightCol">           
      <html:form action="/modifyTemplate">
        <tiles:insert name="wsTemplateTable.tile">
          <tiles:put name="wsListId" value="all_templates"/>
          <tiles:put name="scope" value="all"/>
          <tiles:put name="makeCheckBoxes" value="true"/>
          <tiles:put name="showNames" value="false"/>
          <tiles:put name="showTitles" value="true"/>
          <tiles:put name="showDescriptions" value="true"/>
          <tiles:put name="showSearchBox" value="true"/>
          <tiles:put name="height" value="550"/>
          <tiles:put name="initialFilterText" value="${initialFilterText}"/>
        </tiles:insert>
      </html:form>
</div>
</div>
<script type="text/javascript">
	Nifty("div#pageDesc","big");
</script>
<!-- /templates.jsp -->
