<%--
  - Author: Fengyuan Hu
  - Date: 11-06-2010
  - Copyright Notice:
  - @(#)  Copyright (C) 2002-2010 FlyMine

          This code may be freely distributed and modified under the
          terms of the GNU Lesser General Public Licence.  This should
          be distributed with the code.  See the LICENSE file for more
          information or http://www.gnu.org/copyleft/lesser.html.

  - Description: In this page, users have different options to constrain
                 their query for overlapping located sequence features with
                 the spans they upload.
  --%>

<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!--  spanUploadOptions.jsp -->

<html:xhtml />
<c:set var="bagExampleIdentifiers" value="${WEB_PROPERTIES['bag.example.identifiers']}"/>

<link rel="stylesheet" href="model/collapsibleCheckboxTree/css/jquery.collapsibleCheckboxTree.css" type="text/css" />
<script type="text/javascript" src="model/collapsibleCheckboxTree/js/jquery.collapsibleCheckboxTree.js"></script>
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

   function orgNameChanged(org) {
     //org = org.replace(". ", "\\.\\ ");
       //jQuery('ul#'+org).collapsibleCheckboxTree();

     // Show the tree of selected organism
     loadOrgTree(org)

       }

   jQuery(document).ready(function(){

     // Store org-tree in a 2D array
     orgArray = new Array(${fn:length(orgSet)});

     <c:forEach var="orgMap" items="${orgMap}" varStatus="counter">
       orgArray[${counter.count-1}] = new Array(2);
       orgArray[${counter.count-1}][0] = "${orgMap.key}";
       orgArray[${counter.count-1}][1] = "${orgMap.value}";
     </c:forEach>

     // Get the current organism name in the dropbox
     var orgSelected = jQuery('#orgSelector').find('option').filter(':selected').text();

     // Show the tree of selected organism
     loadOrgTree(orgSelected);

   });

   function loadOrgTree(org) {
       for(i=0; i<orgArray.length; i++) {
           if(orgArray[i][0]==org) {
             jQuery('#tree').html(orgArray[i][1]);
           }
       }
   }
//]]>-->
</script>

<im:boxarea titleKey="bagBuild.makeNewBag" stylename="plainbox" fixedWidth="60%">
  <div class="body">
    <html:form action="/spanUploadAction" method="POST" enctype="multipart/form-data">

      <p><fmt:message key="bagBuild.bagFormText1"/></p>
      <br/>
      <ul>
        <li>Separate identifiers by a <strong>comma</strong>, <strong>space</strong>, <strong>tab</strong> or <strong>new line</strong>.</li>
        <li>Qualify any identifiers that contain whitespace with double quotes like so:  "even skipped".</li>
      </ul>
      <br/>

      <li>
      <label><fmt:message key="bagBuild.bagType"/></label>
      <html:select styleId="orgSelector" property="orgName" onchange="orgNameChanged(this.value);">
      <c:forEach items="${orgSet}" var="orgName">
          <html:option value="${orgName}">${orgName}</html:option>
      </c:forEach>
      </html:select>
      </li>

<div id='tree'>

</div>

   <%-- textarea --%>
   <label><fmt:message key="bagBuild.bagPaste"/></label>

   <span>
   <%-- example bag --%>
   <%--
     <c:set var="bagExampleComment" value="${WEB_PROPERTIES['bag.example.comment']}"/>
     <c:if test="${!empty bagExampleIdentifiers}">
         <div style="text-align:right;width:87%;">
           <html:link href=""
                      onclick="javascript:loadExample('${bagExampleIdentifiers}');return false;">
             (click to see an example)<img src="images/disclosed.gif" title="Click to Show example"/>
           </html:link>
         </div>
     </c:if>
     --%>
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
<!--  /spanUploadOptions.jsp -->