<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>


<!-- templates.jsp -->
<html:xhtml/>

<div class="body">
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

<!-- /templates.jsp -->
