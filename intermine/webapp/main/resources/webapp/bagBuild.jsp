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
      $(open + 'Input').disabled = false;
      $(close + 'Input').disabled = true;
      $(open + 'Type').checked = true;
      $(close + 'Type').checked = false;      
      $('submitBag').disabled = false;
    }

    function clearExample() {
      if($('pasteInput').value == "e.g.: ${bagExampleIdentifiers}") {
         $('pasteInput').value = "";
         $('pasteInput').style.color = "#000";
         $('pasteInput').style.fontStyle = "normal";
      }
    }

    function resetInputs() {
       $('fileInput').disabled = false;
       $('pasteInput').disabled = false;
       $('fileInput').value = '';
       $('pasteInput').value = '';
    }

    function loadExample(example) {
    	switchInputs('paste','file');
      $('pasteInput').focus();
      $('pasteInput').value = example;
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
   }
//]]>-->
</script>

<im:boxarea titleKey="bagBuild.makeNewBag" stylename="plainbox" fixedWidth="60%">
  <div class="body">
    <html:form action="/buildBag" method="post" enctype="multipart/form-data" >
      <p><fmt:message key="bagBuild.bagFormText1"/></p>
      <br/>
      <ul>
        <li>Separate identifiers by a <strong>comma</strong>, <strong>space</strong>, <strong>tab</strong> or <strong>new line</strong>.</li>
        <li>Qualify any identifiers that contain whitespace with double quotes like so:  "even skipped".</li>
      </ul>
      <br/>
      
	  <table class="bagTable">
		  <tr>
		      <td valign="top" class="title">
		          <label><fmt:message key="bagBuild.bagType"/></label>
		      </td>
		      <td>
			      <html:select styleId="typeSelector" property="type" onchange="typeChanged()">
			      <c:forEach items="${preferredTypeList}" var="type">
			          <html:option value="${type}" style="font-weight:bold">${type}</html:option>
			      </c:forEach>
			        <html:option value="" style="text-align:center">----------------</html:option>
			        <c:forEach items="${typeList}" var="type">
			          <html:option value="${type}">${type}</html:option>
			        </c:forEach>
			      </html:select>
			   </td>
		  </tr>
		  <c:if test="${!empty extraBagQueryClass}">
		  <tr>		      
		       <td valign="top" class="title">
		          <label><fmt:message key="bagBuild.extraConstraint">
		          <fmt:param value="${extraBagQueryClass}"/>
		          </fmt:message></label>
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
			   <td valign="top" class="title">
			      <input type="radio" name="uploadType" id="pasteType" value="paste" onclick="switchInputs('paste','file');"/>
			      <label for="pasteType"><fmt:message key="bagBuild.bagPaste"/></label>
			   </td>
			   <td>
				   <%-- example bag --%>
				     <c:set var="bagExampleComment" value="${WEB_PROPERTIES['bag.example.comment']}"/>
				     <c:if test="${!empty bagExampleIdentifiers}">
				         <div style="text-align:right;">
				           <html:link href=""
				                      onclick="javascript:loadExample('${bagExampleIdentifiers}');return false;">
				             (click to see an example)<img src="images/disclosed.gif" title="Click to Show example"/>
				           </html:link>
				         </div>
				     </c:if>
   				   <html:textarea styleId="pasteInput" property="text" rows="10" cols="60" onfocus="if (this.value != '') switchInputs('paste','file');" onkeypress="switchInputs('paste','file');" />
			   </td>
		   </tr>
		     <%-- file input --%>
		    <tr>
		      <td valign="top" class="title">
		          <input type="radio" name="uploadType" id="fileType" value="file" onclick="switchInputs('file','paste');"/>
		          <label for="fileType"><fmt:message key="bagBuild.or"/></label>
		      </td>
		      <td>
		          <html:file styleId="fileInput" property="formFile" onchange="switchInputs('file','paste');" />
		      </td>
		    </tr>
	    </table>
	    
	    <div align="right">
	       <%-- reset button --%>
	       <input type="button" onClick="resetInputs()" value="Reset" />
	       <html:submit styleId="submitBag"><fmt:message key="bagBuild.makeBag"/></html:submit>
	    </div>
	
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
