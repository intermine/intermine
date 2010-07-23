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

<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<!--  spanUploadOptions.jsp -->

<link type="text/css" rel="stylesheet" href="model/jsTree/_docs/syntax/!style.css"/>
<link type="text/css" rel="stylesheet" href="model/jsTree/_docs/!style.css"/>

<script type="text/javascript" src="model/jsTree/_lib/jquery.js"></script>
<script type="text/javascript" src="model/jsTree/_lib/jquery.cookie.js"></script>
<script type="text/javascript" src="model/jsTree/_lib/jquery.hotkeys.js"></script>

<script type="text/javascript" src="model/jsTree/jquery.jstree.js"></script>

<script type="text/javascript" src="model/jsTree/_docs/syntax/!script.js"></script>

<script type="text/javascript" class="source">
<!--//<![CDATA[
   function switchInputs(open, close) {
      jQuery('#' + open + 'Input').attr("disabled","");
      jQuery('#' + close + 'Input').attr("disabled","disabled");
      jQuery('#whichInput').val(open);
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
      if (jQuery("#orgSelector").val() == "C. elegans") {
        jQuery('#pasteInput').val("I:2145137..13728436");}
      else {
        jQuery('#pasteInput').val("2L:10345..15409");}
      return false;
    }

   function orgNameChanged(org) {

     // Show the tree of selected organism
     loadOrgTree(org);

     // jsTree
     jQuery("#tree")
     .bind("loaded.jstree", function (event, data) {
         data.inst.open_all(-1);
       })
     .bind("change_state.jstree", function(event, data) {
         var checked_ids = getCheckedNodeIds();

         var featureTypes = [];
         for(i=0; i<checked_ids.length; i++) {
           <c:forEach var="expFTMap" items="${expFTMap}">
             if(checked_ids[i] == "${expFTMap.key}") {
               <c:forEach var="featureTypeList" items="${expFTMap.value}">
                 featureTypes.push("${featureTypeList}");
               </c:forEach>
             }
           </c:forEach>
         }

         var uniqueFeatureTypes = featureTypes.unique().sort();

         var ftHTMLArray = [];
         for(i=0; i<uniqueFeatureTypes.length; i++) {
           ftHTMLArray.push("<input type='checkbox' name='featureTypes' value='"
                    + uniqueFeatureTypes[i] + "'/>" + uniqueFeatureTypes[i] + "<br/>");
         }

         jQuery("#featureType").html(ftHTMLArray.join(""));
         if(ftHTMLArray.join("") != "") {
             jQuery("#selectFeatureTypes").html("<input type=\"checkbox\" name=\"check\" id=\"check\" onclick=\"checkAll(this.id, 'featureTypes')\"/>Select Feature Types:"); }
           else {
             jQuery("#selectFeatureTypes").html("Select Feature Types:"); }
     })
     .jstree({
         "plugins" : [ "themes", "html_data", "checkbox" ]
     });

   }

   jQuery(document).ready(function(){

     // Store org-tree in a 2D array
     // as array[orgName][HTML]
     orgArray = new Array(${fn:length(orgSet)});

     // Build experiment tree and featureType checkbox
     <c:forEach var="orgName" items="${orgList}" varStatus="counter">
       var treeHTMLArray = [];
       treeHTMLArray.push("<li><p id='selectExperiments'>Select Experiments:</p>");
       treeHTMLArray.push("<div id='tree'>");
       treeHTMLArray.push("<ul id='${orgName}'>");

       <c:forEach var="orgMap" items="${orgMap}">
         if ("${orgMap.key}" == "${orgName}") {
          <c:forEach var="cagMap" items="${orgMap.value}">
            treeHTMLArray.push("<li><a href='#'>");
            treeHTMLArray.push("${cagMap.key}");
            treeHTMLArray.push("</a><ul>");

          <c:forEach var="expMap" items="${cagMap.value}">
            // Link out experiments by right click and open a new page
            treeHTMLArray.push("<li id=\"${expMap.key.name}\"><a href=\"${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}/experiment.do?experiment=${expMap.key.name}\">");
            treeHTMLArray.push("${expMap.key.name}");
            treeHTMLArray.push("</a></li>");
          </c:forEach>
          treeHTMLArray.push("</ul></li>");
          </c:forEach>
         }
       </c:forEach>
       treeHTMLArray.push("</ul>");
       treeHTMLArray.push("</div>");
       treeHTMLArray.push("</li>");

       // Build feature type div
       treeHTMLArray.push("<li>");
       treeHTMLArray.push("<p id='selectFeatureTypes'>Select Feature Types:</p>");
       treeHTMLArray.push("<fieldset>");
       treeHTMLArray.push("<div id='featureType'>");
       // Add content by jQuery according to selected exps
       treeHTMLArray.push("</div>");
       treeHTMLArray.push("</fieldset>");
       treeHTMLArray.push("</li>");

       // Add to array
       orgArray[${counter.count-1}] = new Array(2);
       orgArray[${counter.count-1}][0] = "${orgName}";
       orgArray[${counter.count-1}][1] = treeHTMLArray.join("");
     </c:forEach>

     // Get the current organism name in the dropbox
     var orgSelected = jQuery('#orgSelector').find('option').filter(':selected').text();

     // Show the tree of selected organism
     loadOrgTree(orgSelected);

     jQuery("#tree")
     .bind("loaded.jstree", function (event, data) {
        data.inst.open_all(-1);
      })
     .bind("change_state.jstree", function(event, data) {
         var checked_ids = getCheckedNodeIds();

         var featureTypes = [];
         for(i=0; i<checked_ids.length; i++) {
           <c:forEach var="expFTMap" items="${expFTMap}">
             if(checked_ids[i] == "${expFTMap.key}") {
               <c:forEach var="featureTypeList" items="${expFTMap.value}">
                 featureTypes.push("${featureTypeList}");
               </c:forEach>
             }
           </c:forEach>
         }

         var uniqueFeatureTypes = featureTypes.unique().sort();

         var ftHTMLArray = [];
         for(i=0; i<uniqueFeatureTypes.length; i++) {
           ftHTMLArray.push("<input type='checkbox' name='featureTypes' value='"
                    + uniqueFeatureTypes[i] + "'/>" + uniqueFeatureTypes[i] + "<br/>");
         }

         jQuery("#featureType").html(ftHTMLArray.join(""));
         if(ftHTMLArray.join("") != "") {
           jQuery("#selectFeatureTypes").html("<input type=\"checkbox\" name=\"check\" id=\"check\" onclick=\"checkAll(this.id, 'featureTypes')\"/>Select Feature Types:"); }
         else {
           jQuery("#selectFeatureTypes").html("Select Feature Types:"); }
     })
     .jstree({
         "plugins" : [ "themes", "html_data", "checkbox" ]
     });

   });

   function loadOrgTree(org) {
       for(i=0; i<orgArray.length; i++) {
           if(orgArray[i][0]==org) {
             jQuery('#exp').html(orgArray[i][1]);
           }
       }
   }

   function beforeSubmit() {
     var checked_ids = getCheckedNodeIds();
     jQuery("#hiddenExpFiled").val(checked_ids.join(","))
   }

   function getCheckedNodeIds() {
       var checked_ids = [];
       jQuery("li.jstree-checked").each(function(){
                if(this.id != "") {
                checked_ids.push(this.id);
             }
       });

     return checked_ids;
  }

   // function to remove duplicates from Array
   Array.prototype.unique = function () {
      var r = new Array();
      o:for(var i = 0, n = this.length; i < n; i++)
      {
          for(var x = 0, y = r.length; x < y; x++)
          {
              if(r[x]==this[i])
              {
                  continue o;
              }
          }
          r[r.length] = this[i];
      }
      return r;
  }

   // (un)Check all feature types
   function checkAll(id, name)
   {
     jQuery("input[@name=" + name + "]:checkbox").attr('checked', $('#' + id).is(':checked'));
   }
 //]]>-->
</script>

<html:xhtml />
<c:set var="exampleSpans" value="${exampleSpans}"/>
<%-- <c:set var="bagExampleIdentifiers" value="${WEB_PROPERTIES['bag.example.identifiers']}"/> --%>

<im:boxarea titleKey="spanUpload.makeNewSpan" stylename="plainbox" fixedWidth="60%">
  <div class="body">
    <html:form action="/spanUploadAction" method="POST" enctype="multipart/form-data">

      <p><fmt:message key="spanUpload.spanUploadFormCaption"/></p>
      <br/>
      <ul>
        <li>Span should be <strong>tab delimited</strong> as <strong>chr   start   end</strong> in BED format or <strong>chr:start..end</strong> and separated by a <strong>new line</strong>.</li>
      </ul>
      <br/>

    <ol id="spanUploadlist">
      <li>
      <label>
        <fmt:message key="spanUpload.spanConstraint">
        <fmt:param value="${spanConstraint}"/>
        </fmt:message>
      </label>
      <html:select styleId="orgSelector" property="orgName" onchange="orgNameChanged(this.value);">
      <c:forEach items="${orgList}" var="orgName">
          <html:option value="${orgName}">${orgName}</html:option>
      </c:forEach>
      </html:select>
      </li>

      <div id='exp'><%-- experiments tree and feature types --%></div>
      <input type="hidden" id="hiddenExpFiled" name='experiments' value="">

   <li>
   <%-- textarea --%>
   <label><fmt:message key="spanUpload.spanPaste"/></label>

   <span>

   <%-- example bag --%>
     <%-- <c:set var="bagExampleComment" value="${WEB_PROPERTIES['bag.example.comment']}"/> --%>
     <c:if test="${!empty exampleSpans}">
         <div style="text-align:right;width:87%;">
           <html:link href=""
                      onclick="javascript:loadExample('${exampleSpans}');return false;">
             (click to see an example)<img src="images/disclosed.gif" title="Click to Show example"/>
           </html:link>
         </div>
     </c:if>

   <html:textarea styleId="pasteInput" property="text" rows="10" cols="60" onfocus="if (this.value != '') switchInputs('paste','file');" onkeypress="switchInputs('paste','file');" />

   </span>

   </li>
     <%-- file input --%>
    <li>
      <label><fmt:message key="spanUpload.spanFromFile"/></label>
      <html:file styleId="fileInput" property="formFile" onchange="switchInputs('file','paste');" onkeydown="switchInputs('file','paste');" size="28" />
    </li>
    </ol>

    <div align="right">
       <%-- reset button --%>
       <input type="button" onClick="resetInputs()" value="Reset" />
       <html:submit styleId="submitSpan" onclick="beforeSubmit();"><fmt:message key="spanBuild.search"/></html:submit>
    </div>

    <html:hidden styleId="whichInput" property="whichInput" />

    </html:form>
  </div>
</im:boxarea>

<!--  /spanUploadOptions.jsp -->