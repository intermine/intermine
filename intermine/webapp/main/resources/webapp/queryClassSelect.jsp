<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ page import="java.lang.String" %>

<html:xhtml/>
<!-- queryClassSelect.jsp -->

<script type="text/javascript">
<!--
  // array of help texts built in the controller
  var helpMap = {${helpMap}};

  function showClassSelectHelp() {
      var i = document.queryClassSelectForm.className.selectedIndex;
      var fullSelectedClassName = document.queryClassSelectForm.className[i].value;
      var selectedClassName =
          fullSelectedClassName.substring(fullSelectedClassName.lastIndexOf('.')+1);
      var helpText = helpMap[selectedClassName];
      if (!helpText) {
          helpText = "no description available";
      }
      document.getElementById('queryClassSelect').innerHTML =
          selectedClassName + ":  " + helpText;
      document.getElementById('classSelectDiv').style.display = 'block';
  }

  function handleClassClick(e) {
      if (e.detail == 2) {
          $('queryClassForm').submit();
      }
  }

  window.onload = function() {
      var selector = $('queryClassSelector');
      addEvent(selector, 'click', handleClassClick);
  }
-->
</script>
<div class="body">

  <p>
    <fmt:message key="classChooser.intro"/>
  </p>

  <table border=0>
    <tr>
      <td>
        <html:form styleId="queryClassForm" action="/queryClassSelect">
          <html:select styleId="queryClassSelector" property="className" size="20" onchange="showClassSelectHelp();">
        	<c:forEach items="${preferredTypeList}" var="type">
         	 <html:option value="${type}" style="font-weight:bold">${type}</html:option>
    		</c:forEach>
       		<html:option value="" style="text-align:center">----------------</html:option>
      		<c:forEach items="${typeList}" var="type">
     	     <html:option value="${type}">${type}</html:option>
     	 	</c:forEach>
          </html:select>
          <br/>
          <html:submit>
            <fmt:message key="button.selectClass"/>
          </html:submit>
        </html:form>

      </td>
      <td valign="top" width="100%"align=right>
        <div id="classSelectDiv" style="display:none;">
          <div class="topBar contextHelp"> <%-- IE table width bug --%>
            <table width="98%" cellspacing="0" border="0" cellpadding="0">
              <tr>
                <td valign="top" width="99%">
                  <span id="queryClassSelect"></span>
                </td>
                <td align="right" valign="top"><a href="#" onclick="javascript:document.getElementById('classSelectDiv').style.display='none';return false"><img border="0" src="images/cross.gif" alt="x"></a></td>
              </tr>
            </table>
          </div>
          <br/>
        </div>
      </td>
    </tr>
  </table>

</div>
<!-- /queryClassSelect.jsp -->
