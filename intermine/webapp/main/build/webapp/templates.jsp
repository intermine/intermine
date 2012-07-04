<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

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
          <tiles:put name='filter' value='<%=request.getParameter("filter")%>'/>
          <tiles:put name="templatesPublicPage" value="true"/>
        </tiles:insert>
      </html:form>
</div>

<!-- /templates.jsp -->
