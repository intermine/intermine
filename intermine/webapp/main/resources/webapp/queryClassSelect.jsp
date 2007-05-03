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

</script>
<div class="body">

  <table border=0>
    <tr>
      <td>
        <html:form action="/queryClassSelect">
          <html:select property="className" size="20" onchange="showClassSelectHelp();">
            <c:forEach items="${classes}" var="entry">
              <c:if test="${classCounts[entry.key] > 0}">
                <html:option value="${entry.key}">
                  <c:out value="${entry.value}"/>
                </html:option>
              </c:if>
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
