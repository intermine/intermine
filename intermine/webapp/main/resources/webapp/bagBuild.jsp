<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>



<!-- bagBuild.jsp -->
<html:xhtml/>

<script language="javascript">
<!--//<![CDATA[
   function switchInputs(open, close) {
      document.getElementById(open + 'Input').disabled = false;
      document.getElementById(close + 'Input').disabled = true;
      
      document.getElementById(open + 'Div').style.backgroundColor = '#F5F0FF';
      document.getElementById(close + 'Div').style.backgroundColor = '#fff';
      
      document.getElementById('whichInput').value = open;
      
      document.getElementById('submitBag').disabled = false;
    }
    
    function loadExample(example) {
    	document.getElementById('pasteInput').value = example;    
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

<div id="pageDesc"><p><fmt:message key="bagBuild.intro"/></p></div>
<script type="text/javascript">
	Nifty("div#pageDesc","big");
</script>

<im:roundbox titleKey="bagBuild.makeNewBag" stylename="welcome">
  <div class="bagBuild">
    <html:form action="/buildBag" focus="text" method="post" enctype="multipart/form-data">
      <p><fmt:message key="bagBuild.bagFromText1"/></p>
      <fmt:message key="bagBuild.bagType"/>
      <html:select styleId="typeSelector" property="type" onchange="typeChanged()">
    	<c:forEach items="${preferredTypeList}" var="type">
          <html:option value="${type}" style="font-weight:bold">${type}</html:option>
    	</c:forEach>
        <html:option value="" style="text-align:center">----------------</html:option>
      	<c:forEach items="${typeList}" var="type">
          <html:option value="${type}">${type}</html:option>
      	</c:forEach>
      </html:select>
      <c:if test="${!empty extraBagQueryClass}">
        <p>
          <fmt:message key="bagBuild.extraConstraint">
            <fmt:param value="${extraBagQueryClass}"/>
          </fmt:message>
          <html:select property="extraFieldValue" styleId="extraConstraintSelect" disabled="false">
            <html:option value="" style="text-align:center">----------------</html:option>
      	    <c:forEach items="${extraClassFieldValues}" var="value">
              <html:option value="${value}">${value}</html:option>
      	    </c:forEach>
          </html:select>
        </p>
      </c:if>

    
    <%-- paste header --%>
    <div id="pasteDiv" onclick="switchInputs('paste','file');">
    
     <%-- paste header --%>
    <h4><img src="images/disclosed.gif"/><fmt:message key="bagBuild.bagPaste"/></h4>
    
    <%-- textarea --%>
    <div align="center"><html:textarea styleId="pasteInput" property="text" rows="10" cols="40" /></div>
       
    <%-- example bag --%>
        <c:set var="bagExampleComment" value="${WEB_PROPERTIES['bag.example.comment']}"/>
        <c:set var="bagExampleIdentifiers" value="${WEB_PROPERTIES['bag.example.identifiers']}"/>
        <c:if test="${!empty bagExampleComment && !empty bagExampleIdentifiers}">          
            <div>
              <html:link href=""
                         onmouseover="javascript:$('bagExampleCommentDiv').style.visibility = 'visible'"
                         onmouseout="javascript:$('bagExampleCommentDiv').style.visibility = 'hidden'"
                         onclick="javascript:loadExample('${bagExampleIdentifiers}');return false;">
                (click here to see an example)
              </html:link>
            </div>
            <div id="bagExampleCommentDiv" style="visibility: hidden">
              ${bagExampleComment}
            </div>         
        </c:if>       

    <%-- reset button --%>
    <div align="right"><input type="button" onClick="text.value='';" value="Reset" /></div>
    </div>
    
    <p><fmt:message key="bagBuild.or"/></p>
      
    <%-- file header --%>
   <div id="fileDiv"  onclick="switchInputs('file','paste');">	
	 
	 <%-- file header --%>
     <h4><img src="images/disclosed.gif"/><fmt:message key="bagBuild.bagFromFile"/></h4>
      	
     <%-- file input --%>
     <div align="center"><html:file styleId="fileInput" property="formFile" /></div>
     <br/><br/>
    </div>
      
    <div align="center"><html:submit disabled="true" styleId="submitBag"><fmt:message key="bagBuild.makeBag"/></html:submit></div>    

    <html:hidden styleId="whichInput" property="whichInput" />
  </html:form>
</div>
</im:roundbox>
<!-- /bagBuild.jsp -->
