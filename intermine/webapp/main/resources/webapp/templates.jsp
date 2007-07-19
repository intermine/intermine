<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>


<!-- templates.jsp -->
<html:xhtml/>

<div class="body">
<table padding="0px" margin="0px" width="100%">
  <tr>
    <td valign="top" width="30%">
      <div id="pageDesc"><p><fmt:message key="begin.templates"/></p><br><img src="images/templates_desc.gif" width="328" height="67" alt="Templates Desc"></div>
      <script type="text/javascript">
      	Nifty("div#pageDesc","big");
      </script>
      <div id="actionsLeft">
         <h2>Actions:</h2>
           <html:link action="/summariseAllTemplates" titleKey="begin.summariseAllTemplatesDesc">
             <fmt:message key="begin.summariseAllTemplates"/>
           </html:link>
         <BR/>
           <html:link action="/import" titleKey="begin.importTemplatesDesc">
             <fmt:message key="begin.importTemplates"/>
           </html:link>
      </div>
    </td>
    <td valign="top" width="70%">
       <html:form action="/modifyTemplate">
       <tiles:insert name="wsTemplateTable.tile">
         <tiles:put name="limit" value="15"/>
         <tiles:put name="scope" value="global"/>
         <tiles:put name="makeCheckBoxes" value="true"/>
         <tiles:put name="showNames" value="false"/>
         <tiles:put name="showTitles" value="true"/>
         <tiles:put name="showDescriptions" value="true"/>
         <tiles:put name="showSearchBox" value="true"/>
         <tiles:put name="height" value="300"/>
       </tiles:insert>
       <tiles:insert name="wsTemplateTable.tile">
         <tiles:put name="limit" value="15"/>
         <tiles:put name="scope" value="user"/>
         <tiles:put name="makeCheckBoxes" value="true"/>
         <tiles:put name="showNames" value="false"/>
         <tiles:put name="showTitles" value="true"/>
         <tiles:put name="showDescriptions" value="true"/>
         <tiles:put name="showSearchBox" value="true"/>
         <tiles:put name="height" value="300"/>
       </tiles:insert>
       </html:form>
    </td>
  </tr>
</table>
</div>
<!-- /templates.jsp -->
