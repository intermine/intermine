<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- contextHelp.jsp -->
<div id="ctxHelpDiv" style="display:none">
  <div class="topBar contextHelp">
    <table width="100%" cellspacing="0" border="0" padding="0">
    <tr>
      <td valign="top">
        <div id="ctxHelpTxt"></div>
      </td>
      <td align="right" valign="top">
        <a href="#" onClick="javascript:document.getElementById('ctxHelpDiv').style.display='none';return false">
          <img border="0" src="images/cross.gif" alt="x"/>
        </a>
      </td>
    </tr>
    </table>
  </div>
  <br/>
</div>
<!-- /contextHelp.jsp -->
