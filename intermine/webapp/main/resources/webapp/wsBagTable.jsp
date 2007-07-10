<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<!-- wsBagTable.jsp -->
<tiles:importAttribute name="scope"/>
<tiles:importAttribute name="showDescriptions" ignore="true"/>
<tiles:importAttribute name="limit" ignore="true"/>
<tiles:importAttribute name="tags" ignore="true"/>
<tiles:importAttribute name="makeCheckBoxes" ignore="true"/>

<html:xhtml/>
<tiles:insert name="webSearchableList.tile">
  <tiles:put name="type" value="bag"/>
  <tiles:put name="scope" value="${scope}"/>
  <tiles:put name="tags" value="${tags}"/>
  <tiles:put name="showDescriptions" value="${showDescriptions}"/>
  <tiles:put name="makeCheckBoxes" value="${makeCheckBoxes}"/>
  <tiles:put name="makeTable" value="true"/>
  <tiles:put name="tableHeader" value="wsBagHeader.tile"/>
  <tiles:put name="tableRow" value="wsBagRow.tile"/>
</tiles:insert>
<!-- /wsBagTable.jsp -->
