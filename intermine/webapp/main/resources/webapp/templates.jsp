<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>


<!-- templates.jsp -->
<html:xhtml/>

<div class="body">
<table border=0>
<tr>
<td width="50%" valign="top">
    
      <div id="pageDesc" class="pageDesc"><fmt:message key="begin.templates"/></div>

</td>
<td width="50%" valign="top">

	     <h2>Actions:</h2>
           <html:link action="/summariseAllTemplates" titleKey="begin.summariseAllTemplatesDesc">
             <fmt:message key="begin.summariseAllTemplates"/>
           </html:link>
         <BR/>
           <html:link action="/import" titleKey="begin.importTemplatesDesc">
             <fmt:message key="begin.importTemplates"/>
           </html:link>
    </td>
    </tr>
    </table>
    
    
    <br/>    <br/>
     
       <html:form action="/modifyTemplate">
       <tiles:insert name="wsTemplateTable.tile">
         <tiles:put name="scope" value="all"/>
         <tiles:put name="makeCheckBoxes" value="true"/>
         <tiles:put name="showNames" value="false"/>
         <tiles:put name="showTitles" value="true"/>
         <tiles:put name="showDescriptions" value="true"/>
         <tiles:put name="showSearchBox" value="true"/>
         <tiles:put name="height" value="300"/>
       </tiles:insert>
       </html:form>
   
</div>
<!-- /templates.jsp -->
