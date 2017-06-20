<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<script type="text/javascript" src="js/import.js"></script>

<!-- importQueries.jsp -->
<html:xhtml/>
<div class="body" align="center">
<im:boxarea stylename="plainbox" fixedWidth="60%">

<p><fmt:message key="importQuery.intro"/></p>

<br/>

<p>
  <html:form action="/importQueriesAction?query_build=${param.query_builder}" method="post" enctype="multipart/form-data" >

    <html:hidden property="query_builder" value="${param.query_builder}"/>

    <table id="buildbaglist">
    <tr>
      <td align="right" class="label">
         <label><fmt:message key="importQuery.xml"/></label>
     </td>
      <td><html:textarea styleId="xml" property="xml" rows="20" cols="60"
                   onclick="if(this.value != ''){switchInputs('xml','file');}else{openInputs();}"
                   onkeyup="if(this.value != ''){switchInputs('xml','file');}else{openInputs();}" /></td>
    </tr>
    <tr>
    <%-- file input --%>
    <td align="right" class="label">
        <label><fmt:message key="importTemplates.or"/></label>
    </td>
    <td>
        <html:file styleId="file" property="formFile"
        onchange="switchInputs('file','xml');"
        onkeydown="switchInputs('file','xml');" size="28" />
    </td>
    </tr>
    </table>
    <div align="right">
    <input type="button" onClick="resetInputs('file', 'xml')" value="Reset" />
        <html:submit><fmt:message key="importQuery.submit"/></html:submit>
    </div>
  </html:form>
  <br/>
  <br/>

  <p><font size="0.8em"><fmt:message key="importQuery.message"/></font></p>
</im:boxarea>
</div>
<!-- /importQueries.jsp -->
