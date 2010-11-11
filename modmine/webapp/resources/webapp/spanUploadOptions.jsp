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


<html:xhtml />

<link type="text/css" rel="stylesheet" href="model/jsTree/_docs/syntax/!style.css"/>

<%--
<link type="text/css" rel="stylesheet" href="model/jsTree/_docs/!style.css"/>
<script type="text/javascript" src="model/jsTree/_lib/jquery.js"></script>
--%>

<script type="text/javascript" src="model/jsTree/_lib/jquery.cookie.js"></script>
<script type="text/javascript" src="model/jsTree/_lib/jquery.hotkeys.js"></script>
<script type="text/javascript" src="model/jsTree/jquery.jstree.js"></script>
<script type="text/javascript" src="model/jsTree/_docs/syntax/!script.js"></script>
<script type="text/javascript" src="model/jquery_qtip/jquery.qtip-1.0.js"></script>
<script type="text/javascript" class="source">

<!--//<![CDATA[
   function switchInputs(open, close) {
      jQuery('#' + open + 'Input').attr("disabled","");
      jQuery('#' + close + 'Input').attr("disabled","disabled");
      jQuery('#whichInput').val(open);
    }

    function resetInputs() {
       jQuery('#fileInput').attr("disabled","");
       jQuery('#pasteInput').attr("disabled","");
       jQuery('#fileInput').val('');
       jQuery('#pasteInput').val('');
    }

    function openInputs() {
       jQuery('#fileInput').attr("disabled","");
       jQuery('#pasteInput').attr("disabled","");
    }

    function loadExample() {
      switchInputs('paste','file');
      jQuery('#pasteInput').focus();
      if (jQuery("#orgSelector").val() == "C. elegans") {
        jQuery('#pasteInput').val("I:2145137..2146137\nchrII:3631105-3631106\nIII\t8245810\t8245811\nchrIV\t2263659\t2263660");}
      else {
        jQuery('#pasteInput').val("2L:10345..12409");}
      return false;
    }

   function orgNameChanged(org) {

     // Reset textarea and file input
     resetInputs();

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
           ftHTMLArray.push("<input type='checkbox' checked='yes' class='featureType' name='featureTypes' value='"
                    + uniqueFeatureTypes[i] + "'/>" + uniqueFeatureTypes[i] + "<br/>");
         }

         jQuery("#featureType").html(ftHTMLArray.join(""));
         if(ftHTMLArray.join("") != "") {
             jQuery("#selectFeatureTypes").html("<input type=\"checkbox\" checked=\"yes\" name=\"check\" id=\"check\" onclick=\"checkAll(this.id)\"/>Select Feature Types:"); }
           else {
             jQuery("#selectFeatureTypes").html("Select Feature Types:<br><i>Please select some experiments first</i>"); }
     })
     .jstree({
         "themes" : {
                     "theme" : "apple",
                     "dots" : true,
                     "icons" : false
                     },
         "plugins" : [ "themes", "html_data", "checkbox" ]
     });

   }

   jQuery(document).ready(function(){
     // store expriments with feature types in an array
       expArray = [];

       <c:forEach var="expFTMap" items="${expFTMap}" varStatus="counter">
         expArray.push("${expFTMap.key}");
       </c:forEach>

     // Store org-tree in a 2D array
     // as array[orgName][HTML]
     orgArray = new Array(${fn:length(orgSet)});

     // Build experiment tree and featureType checkbox
     <c:forEach var="orgName" items="${orgList}" varStatus="counter">
       var treeHTMLArray = [];
       treeHTMLArray.push("<p id='selectExperiments' style='padding-bottom: 5px;'>Select Experiments:</p>");
       treeHTMLArray.push("<div id='tree' style='width:740px;'>");
       treeHTMLArray.push("<ul id='${orgName}'>");

       <c:forEach var="orgMap" items="${orgMap}">
         if ("${orgMap.key}" == "${orgName}") {
          <c:forEach var="cagMap" items="${orgMap.value}">
          if ("${cagMap.value}" == "{}") {
              // if exp is null, fix this
              treeHTMLArray.push("<li><i><b>");
              treeHTMLArray.push("${cagMap.key}");
              treeHTMLArray.push("</i></b><ul>");
          }
          else {
              treeHTMLArray.push("<li><a><i><b>");
              treeHTMLArray.push("${cagMap.key}");
              treeHTMLArray.push("</b></i></a><ul>");
          }
          <c:forEach var="expMap" items="${cagMap.value}">
            // Link out experiments by right click and open a new page
            // Check if experiments have feature types
            for (i=0; i<expArray.length; i++) {
              if ("${expMap.key.name}" == expArray[i]) {
                treeHTMLArray.push("<li id=\"${expMap.key.name}\"><a href=\"${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}/experiment.do?experiment=${expMap.key.name}\">");
                treeHTMLArray.push("${expMap.key.name}");
                treeHTMLArray.push("</a></li>");
              }
            }
          </c:forEach>
          treeHTMLArray.push("</ul></li>");
          </c:forEach>
         }
       </c:forEach>
       treeHTMLArray.push("</ul>");
       treeHTMLArray.push("</div>");

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
           ftHTMLArray.push("<input type='checkbox' checked='yes' class='featureType' name='featureTypes' value='"
                    + uniqueFeatureTypes[i] + "' onclick='uncheck(this.checked, \"featureTypes\")'/>" + uniqueFeatureTypes[i] + "<br/>");
         }

         jQuery("#featureType").html(ftHTMLArray.join(""));
         if(ftHTMLArray.join("") != "") {
           jQuery("#selectFeatureTypes").html("<input type=\"checkbox\" checked=\"yes\" name=\"check\" id=\"check\" onclick=\"checkAll(this.id)\"/>Select Feature Types:"); }
         else {
           jQuery("#selectFeatureTypes").html("Select Feature Types:<br><i>Please select some experiments first</i>"); }
     })
     .jstree({
         "themes" : {
                     "theme" : "apple",
                     "dots" : true,
                     "icons" : false
                    },
         "plugins" : [ "themes", "html_data", "checkbox" ]
     });

     // qtip configuration
     jQuery("#baseCorRadioSpan").qtip({
       content: 'e.g. BLAST, GFF/GFF3',
       style: {
         border: {
           width: 3,
           radius: 8,
           color: '#6699CC'
         },
         tip: 'bottomLeft',
         name: 'cream'
       },
        position: {
          corner: {
             target: 'topMiddle',
             tooltip: 'bottomLeft'
          }
        },
       show: 'mouseover',
       hide: 'mouseout'
     });

     jQuery("#interBaseCorRadioSpan").qtip({
       content: 'e.g. UCSC BED, Chado',
       style: {
         border: {
           width: 3,
           radius: 8,
           color: '#6699CC'
         },
         tip: 'topLeft',
         name: 'cream'
       },
       show: 'mouseover',
       hide: 'mouseout'
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
     jQuery("#hiddenExpField").val(checked_ids.join(","))

     var checkedFeatureTypes = [];
     jQuery(".featureType").each(function() {
         if (this.checked) { checkedFeatureTypes.push(this.value); }
       });
     var checkedFeatureTypesToString = checkedFeatureTypes.join(",");

     // validation
     if (jQuery("#hiddenExpField").val() == "") {
       alert("Please select some experiments...");
       return false;
     }

     if (jQuery(".featureType").val() == null || checkedFeatureTypesToString == "") {
       alert("Please select some feature types...");
       return false;
     }

     if (jQuery("#pasteInput").val() == "" && jQuery("#fileInput").val() == "") {
       alert("Please type/paste/upload some genome regions...");
       return false;
     }

     if (jQuery("#pasteInput").val() != "") {
       // Regex validation
       var ddotsRegex = /[^:]+:\d+\.\.\d+$/;
       var tabRegex = /[^\t]+\t\d+\t\d+$/;
       var dashRegex = /[^:]+:\d+\-\d+$/;

       var spanArray = jQuery.trim(jQuery("#pasteInput").val()).split("\n");
       var lineNum;
       for (i=0;i<spanArray.length;i++) {
         lineNum = i + 1;
         if (spanArray[i] == "") {
           alert("Line " + lineNum + " is empty...");
           return false;
         }
         if (!spanArray[i].match(ddotsRegex) && !spanArray[i].match(tabRegex) && !spanArray[i].match(dashRegex)) {
           alert(spanArray[i] + " doesn't match any supported format...");
           return false;
         }
       }
     }
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

   // (un)Check all featureType checkboxes
   function checkAll(id)
   {
     jQuery(".featureType").attr('checked', jQuery('#' + id).is(':checked'));
     jQuery("#check").css("opacity", 1);
   }

   // check/uncheck any featureType checkbox
   function uncheck(status, name)
   {
     var statTag;
     if (!status) { //unchecked
       jQuery(".featureType").each(function() {
         if (this.checked) {statTag=true;}
       });

       if (statTag) {
        jQuery("#check").attr('checked', true);
        jQuery("#check").css("opacity", 0.5); }
       else {
        jQuery("#check").removeAttr('checked');
        jQuery("#check").css("opacity", 1);}
     }
     else { //checked
       jQuery(".featureType").each(function() {
         if (!this.checked) {statTag=true;}
     });

     if (statTag) {
       jQuery("#check").attr('checked', true);
       jQuery("#check").css("opacity", 0.5); }
     else {
       jQuery("#check").attr('checked', true);
       jQuery("#check").css("opacity", 1);}
     }
}


 //]]>-->
</script>

<div align="center" style="padding-top: 20px;">
<im:boxarea titleKey="spanUpload.makeNewSpan" stylename="plainbox" fixedWidth="85%" titleStyle="font-size: 1.2em; text-align: center;">
  <div class="body">
    <html:form styleId="spanForm" action="/spanUploadAction" method="POST" enctype="multipart/form-data">

      <p><fmt:message key="spanUpload.spanUploadFormCaption"/></p>
      <br/>
      <ul>
         <li>Genome regions in the following formats are accepted:
           <ul>
            <li><b>chromosome:start..end</b>, e.g. <i>2L:11334..12296</i></li>
            <li><b>chromosome:start-end</b>, e.g. <i>2R:5866746-5868284</i> or <i>chrII:14646344-14667746</i></li>
            <li><b>tab delimited</b></li>
           </ul>
        <li>Both <b>base coordinate</b> (e.g. BLAST, GFF/GFF3) and <b>interbase coordinate</b> (e.g. UCSC BED, Chado) systems are supported, users need to explicitely select one. By default, the base coordinate is selected.</li>
        <li>Each genome region needs to take a <b>new line</b>.</li>
        <li>Only experiments with features are listed below.</li>
        <li>Right click <ba experiment</b> in the tree to go to experiment report page.</li>
      </ul>
      <br/>

    <ol id="spanUploadlist">

   <%-- organism --%>
   <li>
      <span>
        <fmt:message key="spanUpload.spanConstraint">
        <fmt:param value="${spanConstraint}"/>
        </fmt:message>
      </span>
      <html:select styleId="orgSelector" property="orgName" onchange="orgNameChanged(this.value);">
      <c:forEach items="${orgList}" var="orgName">
          <html:option value="${orgName}">${orgName}</html:option>
      </c:forEach>
      </html:select>
   </li>
   <%-- organism --%>
   <br/>

   <%-- experiments tree and feature types --%>
    <li>
      <div id='exp'></div>
      <input type="hidden" id="hiddenExpField" name='experiments' value="">
    </li>
    <br/>

    <li>
      <p id='selectFeatureTypes'></p>
      <table cellpadding='0' cellspacing='0' border='0'>
        <div id='featureType'></div>
      </table>
    </li>
   <%-- experiments tree and feature types --%>
    <br/>

   <li>
   <%-- textarea --%>
   <span><fmt:message key="spanUpload.spanPaste"/></span> in
   <span id="baseCorRadioSpan"><html:radio property="isInterBaseCoordinate" value="isNotInterBaseCoordinate"> base coordinate</html:radio></span>
   <span id="interBaseCorRadioSpan"><html:radio property="isInterBaseCoordinate" value="isInterBaseCoordinate"> interbase coordinate</html:radio></span>

   <%-- example span --%>
     <div style="text-align:left;">
       <html:link href=""
                  onclick="javascript:loadExample();return false;">
         (click to see an example)<img src="images/disclosed.gif" title="Click to Show example"/>
       </html:link>
     </div>
     <html:textarea styleId="pasteInput" property="text" rows="10" cols="60" onclick="if(this.value != ''){switchInputs('paste','file');}else{openInputs();}" onkeyup="if(this.value != ''){switchInputs('paste','file');}else{openInputs();}" />

   <br>
   <%-- file input --%>

     <span><fmt:message key="spanUpload.spanFromFile"/></span><br>
     <html:file styleId="fileInput" property="formFile" onchange="switchInputs('file','paste');" onkeydown="switchInputs('file','paste');" size="28" />
     <html:hidden styleId="whichInput" property="whichInput" />
   </li>

   </ol>

    <div align="right">
       <%-- reset button --%>
       <input type="button" onClick="resetInputs()" value="Reset" />
       <html:submit styleId="submitSpan" onclick="javascript: return beforeSubmit();"><fmt:message key="spanBuild.search"/></html:submit>
    </div>

    </html:form>
  </div>
</im:boxarea>
</div>

<!--  /spanUploadOptions.jsp -->
