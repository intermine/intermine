<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<!-- wsTemplateTable.jsp -->
<tiles:importAttribute name="wsListId"/>
<tiles:importAttribute name="scope"/>
<tiles:importAttribute name="showNames" ignore="true"/>
<tiles:importAttribute name="showTitles" ignore="true"/>
<tiles:importAttribute name="showDescriptions" ignore="true"/>
<tiles:importAttribute name="limit" ignore="true"/>
<tiles:importAttribute name="height" ignore="true"/>
<tiles:importAttribute name="tags" ignore="true"/>
<tiles:importAttribute name="makeCheckBoxes" ignore="true"/>
<tiles:importAttribute name="showSearchBox" ignore="true"/>

<html:xhtml/>

    <script LANGUAGE="JavaScript">
      <!--//<![CDATA[
          function confirmAction() {
          return confirm("Do you really want to delete the selected templates?")
          }
          //]]>-->
    </script>

<im:roundbox titleKey="wsTemplateTable.heading" stylename="welcome">
<div style=""><fmt:message key="templates.intro"/></div>
<div class="" id="ws_${wsListId}_template">
<tiles:insert name="wsFilterList.tile">
  <tiles:put name="type" value="template"/>
  <tiles:put name="wsListId" value="${wsListId}"/>
  <tiles:put name="scope" value="${scope}"/>
  <tiles:put name="tags" value="${tags}"/>
  <tiles:put name="showNames" value="${showNames}"/>
  <tiles:put name="showTitles" value="${showTitles}"/>
  <tiles:put name="showDescriptions" value="${showDescriptions}"/>
  <tiles:put name="makeCheckBoxes" value="${makeCheckBoxes}"/>
  <tiles:put name="makeLine" value="true"/>
  <tiles:put name="makeTable" value="false"/>
  <!-- <tiles:put name="wsHeader" value="wsTemplateHeader.tile"/> -->
  <tiles:put name="wsRow" value="wsTemplateLine.tile"/>
  <tiles:put name="limit" value="${limit}"/>
  <tiles:put name="height" value="${height}"/>
  <tiles:put name="showSearchBox" value="${showSearchBox}"/>
</tiles:insert>
</div>
</im:roundbox>



  <p width="100%" align="right">
        <html:submit property="export" styleId="export_button">
          <fmt:message key="history.exportSelected"/>
        </html:submit>
         <html:hidden property="pageName" value="templates"/>
  </p>


<!-- /wsTemplateTable.jsp -->
