<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>
<%@ page import="java.lang.String" %>

<html:xhtml/>
<!-- queryClassSelect.jsp -->

<script type="text/javascript">
<!--
  // array of help texts built in the controller
  var helpMap = {${helpMap}};

  function showClassSelectHelp() {
      var fullSelectedClassName = jQuery("#queryClassSelector").val();
      var selectedClassName =
          fullSelectedClassName.substring(fullSelectedClassName.lastIndexOf('.')+1);
      if (selectedClassName.length > 0) {
          var helpText = helpMap[selectedClassName];
          document.getElementById('queryClassSelect').innerHTML =
              selectedClassName + ":  " + helpText;
          document.getElementById('classSelectDiv').style.display = 'block';
          jQuery('#submitClassSelect').attr('disabled', false);
      } else {
          jQuery('#classSelectDiv').hide();
          jQuery('#submitClassSelect').attr('disabled', true);
      }
  }

  function handleClassClick(e) {
      if (e.detail == 2 &&  jQuery("#queryClassSelector").val() != '') {
          jQuery('#queryClassForm').submit();
      }
  }

  window.onload = function() {
      jQuery('#queryClassSelector').click(function(e){
		handleClassClick(e);
      });
      jQuery('#submitClassSelect').attr('disabled', true);
      
      <!-- dbl click to submit the form as well -->

      (function($) {
        // Previously clicked option.
        var option = null,
          timeout = null;
        // If you'd like to replace this with `dblclick` then think again.
        $('select#queryClassSelector option').on('click', function(evt) {
          // Clear any timeouts.
          clearTimeout(timeout);
          // This value.
          var value = $(evt.target).val();
          // You made it, go kiss the princess...
          if (value == option) {
            $('form#queryClassSelectForm').submit();
          } else {
            option = value;
            // Back to square 1.
            timeout = setTimeout(function() {
              option = null;
            }, 500);
          }
        });
      })(window.jQuery)
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
        <html:form action="/queryClassSelect">
          <html:select styleId="queryClassSelector" property="className" size="10" onchange="showClassSelectHelp();">
          <c:forEach items="${preferredTypeList}" var="type">
            <html:option value="${type}" style="font-weight:bold">${imf:formatPathStr(type, INTERMINE_API, WEBCONFIG)}</html:option>
          </c:forEach>
           <html:option value="" style="text-align:center">----------------</html:option>
          <c:forEach items="${typeList}" var="type">
            <html:option value="${type}">${imf:formatPathStr(type, INTERMINE_API, WEBCONFIG)}</html:option>
          </c:forEach>
          </html:select>
          <br/>
          <html:submit styleId="submitClassSelect">
            <fmt:message key="button.selectClass"/>
          </html:submit>
        </html:form>

      </td>
      <td valign="top" width="100%" align="right">
        <div id="classSelectDiv" style="display:none;">
          <div class="topBar contextHelp"> <%-- IE table width bug --%>
            <table width="98%" cellspacing="0" border="0" cellpadding="0">
              <tr>
                <td valign="top" width="99%">
                  <span id="queryClassSelect"></span>
                </td>
                <td align="right" valign="top">
                    <a href="#" onclick="javascript:document.getElementById('classSelectDiv').style.display='none';return false">
                        <img border="0" src="images/cross.gif" title="Click here to close the help text."/>
                    </a>
                </td>
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
