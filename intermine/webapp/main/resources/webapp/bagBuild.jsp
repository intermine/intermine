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
      document.getElementById(open + 'Input').disabled = false;
      document.getElementById(close + 'Input').disabled = true;

      document.getElementById('whichInput').value = open;

      document.getElementById('submitBag').disabled = false;
      // only clear contents if contents equals our help text
      
      if(open == 'paste' && document.getElementById('pasteInput').value == "e.g.: ${bagExampleIdentifiers}") {
         document.getElementById('pasteInput').value = "";
         document.getElementById('pasteInput').style.color = "#000";
         document.getElementById('pasteInput').style.fontStyle = "normal";
      }
    }

    function resetInputs() {
       document.getElementById('fileInput').disabled = false;
       document.getElementById('pasteInput').disabled = false;
       document.getElementById('fileInput').value='';
       initPasteInput();
    }

    function initPasteInput() {
       document.getElementById('pasteInput').value = "e.g.: ${bagExampleIdentifiers}";
       document.getElementById('pasteInput').style.color = "#666";
       document.getElementById('pasteInput').style.fontStyle = "italic";
    }

    function loadExample(example) {
    	document.getElementById('pasteInput').focus();
    	document.getElementById('pasteInput').value = example;
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

   window.onload = function() {
     typeChanged();
   }

//]]>-->
</script>


<div class="actionArea" style="width:60%;clear:both;text-align:left">
  <h2><fmt:message key="bagBuild.makeNewBag"/></h2>
  <div class="bagBuild">
    <html:form action="/buildBag" method="post" enctype="multipart/form-data" >
      <p><fmt:message key="bagBuild.bagFromText1"/></p>
      <%-- example bag --%>
          <c:set var="bagExampleComment" value="${WEB_PROPERTIES['bag.example.comment']}"/>
          <c:if test="${!empty bagExampleComment && !empty bagExampleIdentifiers}">
              <div style="align: right">
                <html:link href=""
                           onclick="javascript:loadExample('${bagExampleIdentifiers}');return false;">
                  (click to see an example)
                </html:link>
              </div>
          </c:if>
          <br/>
<ol id="buildbaglist">
  <li>
      <label><fmt:message key="bagBuild.bagType"/></label>
      <html:select styleId="typeSelector" property="type" onchange="typeChanged()">
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
            <html:option value="">All</html:option>
      	    <c:forEach items="${extraClassFieldValues}" var="value">
              <html:option value="${value}">${value}</html:option>
      	    </c:forEach>
          </html:select>
      </c:if>
   </li>
   <li>
   <%-- textarea --%>
   <label><fmt:message key="bagBuild.bagPaste"/></label>
   <html:textarea styleId="pasteInput" property="text" rows="10" cols="35" onfocus="switchInputs('paste','file');" />
   <script type="text/javascript" charset="utf-8">
      initPasteInput();
    </script>
   </li>
	 <%-- file header --%>
     <!-- <h4><img src="images/disclosed.gif"/><fmt:message key="bagBuild.bagFromFile"/></h4> -->

     <%-- file input --%>
    <li>
      <label><fmt:message key="bagBuild.or"/></label>
      <html:file styleId="fileInput" property="formFile" onkeypress="switchInputs('file','paste');"  onfocus="switchInputs('file','paste');" />
    </li>
    </ol>
    <div align="right">
       <%-- reset button --%>
       <input type="button" onClick="resetInputs()" value="Reset" />
       <html:submit disabled="true" styleId="submitBag"><fmt:message key="bagBuild.makeBag"/></html:submit>
    </div>

    <html:hidden styleId="whichInput" property="whichInput" />
  </html:form>
</div>
</div>
<!-- /bagBuild.jsp -->
