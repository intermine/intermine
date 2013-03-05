<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<script type="text/javascript" src="js/import.js"></script>

<!-- importTemplates.jsp -->
<html:xhtml/>

<div class="body" align="center">
<im:boxarea stylename="plainbox" fixedWidth="60%">

<p><fmt:message key="importTemplates.intro"/></p>

<br/>

<p>
  <html:form action="/importTemplates" method="post" enctype="multipart/form-data" >
    <table id="buildbaglist">
    <tr>
      <td align="right" class="label">
         <label><fmt:message key="importTemplates.xml"/></label>
     </td>
      <td><html:textarea styleId="xml" property="xml" rows="20" cols="60"
                   onclick="if(this.value != ''){switchInputs('xml','file');}else{openInputs('xml','file');}"
                   onkeyup="if(this.value != ''){switchInputs('xml','file');}else{openInputs('xml','file');}" />
      </td>
    </tr>
    <tr>
    <%-- file input --%>
    <td align="right" class="label">
        <label><fmt:message key="importQuery.or"/></label>
    </td>
    <td>
        <html:file styleId="file" property="formFile"
        onchange="switchInputs('file','xml');"
        onkeydown="switchInputs('file','xml');" size="28" />
    </td>
    </tr>
    <c:if test="${IS_SUPERUSER}">
      <tr>
        <td></td>
        <td align="right" valign="top"><fmt:message key="importTemplates.overwriting"/> <html:checkbox property="overwriting"/></td>
      </tr>
      <tr>
        <td></td>
        <td align="right" valign="top"><fmt:message key="importTemplates.deleteTracks"/> <html:checkbox property="deleteTracks"/></td>
      </tr>
    </c:if>
    </table>
    <div align="right">
    <input type="button" onClick="resetInputs('file', 'xml')" value="Reset" />
        <html:submit><fmt:message key="importTemplates.submit"/></html:submit>
    </div>
  </html:form>

  <br/>
  <br/>

  <font size="0.8em"><fmt:message key="importTemplates.message"/></font>
</im:boxarea>
</div>
<!-- /importTemplates.jsp -->
