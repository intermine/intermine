<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>



<!-- bagBuild.jsp -->
<html:xhtml/>
<c:set var="bagExampleIdentifiers" value="${WEB_PROPERTIES['bag.example.identifiers']}"/>

<script language="javascript">
<!--//<![CDATA[
   function switchInputs(open, close) {
      jQuery('#' + open + 'Input').attr("disabled","");
      jQuery('#' + close + 'Input').attr("disabled","disabled");
    }

    function clearExample() {
      if(jQuery('#pasteInput').val() == "e.g.: ${bagExampleIdentifiers}") {
         jQuery('#pasteInput').val("");
         jQuery('#pasteInput').css("color", "#000");
         jQuery('#pasteInput').css("fontStyle", "normal");
      }
    }

    function resetInputs() {
       jQuery('#fileInput').attr("disabled","");
       jQuery('#pasteInput').attr("disabled","");
       jQuery('#fileInput').val('');
       jQuery('#pasteInput').val('');
    }

    function loadExample(example) {
      switchInputs('paste','file');
      jQuery('#pasteInput').focus();
      jQuery('#pasteInput').val(example);
      return false;
    }

   var typeToEnable = new Array();
   <c:forEach items="${typesWithConnectingField}" var="type">
   typeToEnable['${type}'] = 1;
   </c:forEach>

   function typeChanged() {
     var type = document.getElementById('typeSelector').value;
     var el = document.getElementById('extraConstraintSelect');
     if (typeToEnable[type] == null){
        el.disabled = true;
     } else {
        el.disabled = false;
     }
     if (type.length > 0){
        jQuery('#submitBag').attr('disabled', '');
     } else {
        jQuery('#submitBag').attr('disabled', 'disabled');
     }
   }
//]]>-->
</script>


<im:boxarea titleKey="bagBuild.makeNewBag" stylename="plainbox" fixedWidth="60%">
  <div class="body">
    <html:form action="/buildBag" method="post" enctype="multipart/form-data" >
      <p><fmt:message key="bagBuild.bagFormText1"/></p>
      <br/>
      <p><fmt:message key="bagBuild.helpText"/></p>
      <br/>
<ol id="buildbaglist">
  <li>
      <label><fmt:message key="bagBuild.bagType"/></label>
      <html:select styleId="typeSelector" property="type" onchange="typeChanged();">
      <c:forEach items="${preferredTypeList}" var="type">
          <html:option value="${type}" style="font-weight:bold">${type}</html:option>
      </c:forEach>
        <html:option value="" style="text-align:center">----------------</html:option>
        <c:forEach items="${typeList}" var="type">
          <html:option value="${type}">${type}</html:option>
        </c:forEach>
      </html:select>
  </li>
  <li>
      <c:if test="${!empty extraBagQueryClass}">
       <label>
         <fmt:message key="bagBuild.extraConstraint">
           <fmt:param value="${extraBagQueryClass}"/>
         </fmt:message>
       </label>
         <html:select property="extraFieldValue" styleId="extraConstraintSelect" disabled="false" >
           <html:option value="">Any</html:option>
           <c:forEach items="${extraClassFieldValues}" var="value">
             <html:option value="${value}">${value}</html:option>
           </c:forEach>
         </html:select>
      </c:if>
   </li>
   <li>
   <%-- textarea --%>
   <label><fmt:message key="bagBuild.bagPaste"/></label>
   <span>
   <%-- example bag --%>
     <c:set var="bagExampleComment" value="${WEB_PROPERTIES['bag.example.comment']}"/>
     <c:if test="${!empty bagExampleIdentifiers}">
         <div style="text-align:right;width:87%;">
           <html:link href=""
                      onclick="javascript:loadExample('${bagExampleIdentifiers}');return false;">
             (click to see an example)<img src="images/disclosed.gif" title="Click to Show example"/>
           </html:link>
         </div>
     </c:if>
   <html:textarea styleId="pasteInput" property="text" rows="10" cols="60" onfocus="if (this.value != '') switchInputs('paste','file');" onkeypress="switchInputs('paste','file');" />
   </span>
   </li>
     <%-- file input --%>
    <li>
      <label><fmt:message key="bagBuild.or"/></label>
      <html:file styleId="fileInput" property="formFile" onchange="switchInputs('file','paste');" onkeydown="switchInputs('file','paste');" size="28" />
    </li>
    </ol>
    <div align="right">
       <%-- reset button --%>
       <input type="button" onClick="resetInputs()" value="Reset" />
       <html:submit styleId="submitBag"><fmt:message key="bagBuild.makeBag"/></html:submit>
    </div>

    <html:hidden styleId="whichInput" property="whichInput" />
  </html:form>
</div>
</im:boxarea>

<script language="javascript">
<!--//<![CDATA[
   window.onload = function() {
     typeChanged();
   }

//]]>-->
</script>
<!-- /bagBuild.jsp -->
