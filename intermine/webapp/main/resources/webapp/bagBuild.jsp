<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>



<!-- bagBuild.jsp -->
<html:xhtml/>

<script language="javascript">
<!--//<![CDATA[

  <%-- Java to JavaScript map --%>
  <c:if test="${!empty bagExampleIdentifiers && !empty bagExampleIdentifiers['default']}">
    var bagExampleIdentifiers = {};
    <c:forEach items="${bagExampleIdentifiers}" var="exampleType">
      bagExampleIdentifiers['${exampleType.key}'.replace(/\s/gi,"").toLowerCase()] = '${exampleType.value}';
    </c:forEach>
  </c:if>

   function switchInputs(open, close) {
      jQuery('#' + open + 'Input').attr("disabled", false);
      jQuery('#' + close + 'Input').attr("disabled", false);
    }

    function resetInputs() {
       jQuery('#fileInput').attr("disabled", false);
       jQuery('#pasteInput').attr("disabled", false);
       jQuery('#fileInput').val('');
       jQuery('#pasteInput').val('');
    }

    function openInputs() {
       jQuery('#fileInput').attr("disabled", false);
       jQuery('#pasteInput').attr("disabled", false);
    }

    function loadExample() {
      switchInputs('paste','file');
      jQuery('#pasteInput').focus();

      <%-- based on the selected type, load an example list identifiers --%>
      var example = bagExampleIdentifiers[jQuery("select#typeSelector option:selected").val().toLowerCase()];
      if (!example) {
        example = bagExampleIdentifiers['default'];
      }
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
        jQuery('#submitBag').attr('disabled', false);
     } else {
        jQuery('#submitBag').attr('disabled', true);
     }
   }
//]]>-->
</script>

<div id="list-progress">
  <div class="wrap" align="center">
    <div class="white-start">&nbsp;</div><div class="white"><strong>1</strong> <span>Upload list of identifiers</span></div
    ><div class="white-to-gray">&nbsp;</div
    ><div class="gray"><strong>2</strong> <span>Verify identifier matches</span></div><div class="gray-to-gray">&nbsp;</div
    ><div class="gray"><img src="images/icons/lists-16.png" alt="list" /> <span>List analysis</span></div>
  </div>
</div>
<div class="clear">&nbsp;</div>

<im:boxarea titleImage="lists-64.png" titleKey="bagBuild.makeNewBag" stylename="plainbox" fixedWidth="60%">
  <div class="body">
    <html:form action="/buildBag" method="post" enctype="multipart/form-data" >
      <p><fmt:message key="bagBuild.bagFormText1"/></p>
      <br/>
      <p><fmt:message key="bagBuild.helpText"/></p>
      <br/>

    <!-- create a new list table -->
    <table id="buildbaglist">
        <tr>
            <td align="right" class="label">
                <label><fmt:message key="bagBuild.bagType"/></label>
            </td>
            <td>
                  <html:select styleId="typeSelector" property="type" onchange="typeChanged();">
                      <c:forEach items="${preferredTypeList}" var="type">
                      <html:option value="${type}" style="font-weight:bold"><c:out value="${imf:formatPathStr(type, INTERMINE_API, WEBCONFIG)}"/></html:option>
                      </c:forEach>
                    <html:option value="" style="text-align:center">----------------</html:option>
                    <c:forEach items="${typeList}" var="type">
                          <html:option value="${type}"><c:out value="${imf:formatPathStr(type, INTERMINE_API, WEBCONFIG)}"/></html:option>
                    </c:forEach>
                  </html:select>
            </td>
          </tr>
        <c:if test="${!empty extraBagQueryClass}">
            <tr>
                <td align="right" class="label">
                       <label>
                         <fmt:message key="bagBuild.extraConstraint">
                               <fmt:param value="${extraBagQueryClass}"/>
                         </fmt:message>
                       </label>
                   </td>
                   <td>
                     <html:select property="extraFieldValue" styleId="extraConstraintSelect" disabled="false" >
                           <html:option value="">Any</html:option>
                           <c:forEach items="${extraClassFieldValues}" var="value">
                             <html:option value="${value}">${value}</html:option>
                           </c:forEach>
                     </html:select>
                 </td>
            </tr>
        </c:if>
           <tr>
               <%-- textarea --%>
               <td align="right" class="label">
                   <label><fmt:message key="bagBuild.bagPaste"/></label>
               </td>
               <td>
                 <c:set var="bagExampleComment" value="${WEB_PROPERTIES['bag.example.comment']}"/>
                 <c:if test="${!empty bagExampleIdentifiers && !empty bagExampleIdentifiers['default']}">
                       <html:link href="" onclick="javascript:loadExample();return false;">
                         (click to see an example)<img src="images/disclosed.gif" title="Click to Show example"/>
                       </html:link>
                 </c:if>
                   <html:textarea styleId="pasteInput" property="text" rows="10"
                   onclick="if(this.value != ''){switchInputs('paste','file');}else{openInputs();}"
                   onkeyup="if(this.value != ''){switchInputs('paste','file');}else{openInputs();}" />
               </td>
           </tr>
           <tr>
               <%-- file input --%>
               <td align="right" class="label">
                   <label><fmt:message key="bagBuild.or"/></label>
               </td>
               <td>
                   <html:file styleId="fileInput" property="formFile"
                   onchange="switchInputs('file','paste');"
                   onkeydown="switchInputs('file','paste');" size="28" />
               </td>
           </tr>
           <tr>
               <td></td>
               <td class="label"><html:checkbox property="caseSensitive"/>&nbsp;<label><fmt:message key="bagBuild.caseSensitive"/></label></td>
           </tr>
       </table>

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
